package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.comprasco.bakeprofit.service.BrandService;

/**
 * Dos peticiones que crean la misma marca a la vez obtienen la misma fila
 * (INSERT ... ON CONFLICT DO NOTHING + búsqueda con lower()).
 *
 * <p>
 * Necesita PostgreSQL real, por eso no corre con {@code mvn test}: tiene la etiqueta
 * "integration", que el pom excluye por defecto. Solo se habilita si existe la
 * variable SPRING_DATASOURCE_URL, para no caer en la base del perfil local (la
 * Supabase compartida). Apunta a la base de pruebas, no a la compartida:
 * </p>
 *
 * <pre>
 * $env:SPRING_DATASOURCE_URL="jdbc:postgresql://&lt;host-pruebas&gt;:5432/postgres"
 * $env:SPRING_DATASOURCE_USERNAME="&lt;usuario&gt;"; $env:SPRING_DATASOURCE_PASSWORD="&lt;clave&gt;"
 * $env:GOOGLE_CLIENT_ID="&lt;el-tuyo&gt;"
 * ./mvnw test "-Dgroups=integration" "-Dsurefire.excludedGroups="
 * </pre>
 *
 * <p>
 * Crea una sola marca con nombre único y la borra al terminar.
 * </p>
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
// Dos transacciones quedan abiertas a la vez; el pool del perfil local (2) no deja margen
@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=4")
class BrandConcurrencyIntegrationTest {

    private static final Duration FIRST_TRANSACTION_HOLD = Duration.ofMillis(1500);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String BRAND_NAME_PREFIX = "Marca IT ";
    private static final String COUNT_BY_NAME =
            "SELECT count(*) FROM brands WHERE lower(name) = lower(?)";
    private static final String DELETE_BY_NAME =
            "DELETE FROM brands WHERE lower(name) = lower(?)";

    @Autowired
    private BrandService brandService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void dosPeticionesSimultaneasObtienenLaMismaMarca() throws Exception {
        String brandName = BRAND_NAME_PREFIX + UUID.randomUUID();
        CountDownLatch firstInserted = new CountDownLatch(1);
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            // La primera petición registra la marca y mantiene su transacción abierta
            Future<Long> first = threads.submit(() -> transactionTemplate.execute(status -> {
                Long id = brandService.resolveByName(brandName).getId();
                firstInserted.countDown();
                sleep(FIRST_TRANSACTION_HOLD);
                return id;
            }));
            // La segunda llega mientras tanto, con otras mayúsculas
            Future<TimedResult> second = threads.submit(() -> {
                firstInserted.await();
                long start = System.nanoTime();
                Long id = transactionTemplate.execute(status ->
                        brandService.resolveByName(brandName.toUpperCase(Locale.ROOT)).getId());
                return new TimedResult(id, Duration.ofNanos(System.nanoTime() - start));
            });

            Long firstId = first.get(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            TimedResult secondResult = second.get(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            assertThat(secondResult.id()).isEqualTo(firstId);
            // Esperó a que la primera confirmara; se mide contra la mitad para no fallar en máquinas lentas
            assertThat(secondResult.elapsed()).isGreaterThanOrEqualTo(FIRST_TRANSACTION_HOLD.dividedBy(2));
            assertThat(jdbcTemplate.queryForObject(COUNT_BY_NAME, Long.class, brandName)).isEqualTo(1L);
        } finally {
            threads.shutdownNow();
            jdbcTemplate.update(DELETE_BY_NAME, brandName);
        }
    }

    private record TimedResult(Long id, Duration elapsed) {
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}