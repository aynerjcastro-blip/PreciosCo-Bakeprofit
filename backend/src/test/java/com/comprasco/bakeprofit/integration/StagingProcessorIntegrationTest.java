package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import com.comprasco.bakeprofit.entity.OfferStatus;
import com.comprasco.bakeprofit.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.repository.PendingOfferKey;
import com.comprasco.bakeprofit.repository.ScrapedOfferRepository;
import com.comprasco.bakeprofit.service.OfferIngestionService.Result;
import com.comprasco.bakeprofit.service.StagingProcessor;

/**
 * El procesador contra PostgreSQL real: crea productos, abre y refresca periodos de
 * precio, marca estados, publica el evento después del commit y no duplica nada al
 * repetirse.
 *
 * <p>
 * Mismo mecanismo que BrandConcurrencyIntegrationTest: etiqueta "integration" (fuera de
 * {@code mvn test}) y solo corre con SPRING_DATASOURCE_URL, con el script
 * {@code 2026-11-ht10-prices-last-seen.sql} ya aplicado. Solo procesa las ofertas que
 * inserta ({@code processOffers} con sus propios ids) y nunca llama a {@code runOnce} ni
 * a la retención: no toca ofertas ni filas de nadie más, y borra lo que creó.
 * </p>
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest(properties = "preciosco.staging.enabled=false")
@RecordApplicationEvents
class StagingProcessorIntegrationTest {

    private static final String COLOMBIA_PREFIX = "770";
    private static final int EAN_13_BODY_LENGTH = 12;
    private static final BigDecimal PRICE = new BigDecimal("4500.00");
    private static final BigDecimal LOWER_PRICE = new BigDecimal("4200.00");
    private static final String INVALID_EAN = "7702552000142";
    // Fecha que el scraper nunca escribe: entre dos ofertas de la prueba no cabe ninguna real
    private static final LocalDateTime KEYSET_BASE = LocalDateTime.of(2001, 1, 1, 0, 0);

    private static final String INSERT_OFFER = """
        INSERT INTO scraped_offers (store_id, scrape_target_id, ean, name, brand_name, price,
                                    product_url, image_url, net_content, unit_base, scraped_at, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'G', ?, 'PENDING')
        RETURNING id
        """;
    private static final String PRICES_OF_PRODUCT = """
        SELECT p.value, p.registration_date, p.last_seen_at, p.source, p.product_url
        FROM prices p JOIN products pr ON pr.id = p.product_id
        WHERE pr.ean = ? ORDER BY p.registration_date, p.id
        """;

    @Autowired
    private StagingProcessor processor;
    @Autowired
    private ScrapedOfferRepository offerRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ApplicationEvents events;

    private String suffix;
    private Long rootId;
    private Long leafId;
    private Long storeId;
    private Long targetId;
    private String brandName;
    private String eanRice;
    private String eanBeans;

    @BeforeEach
    void createCatalogData() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        brandName = "Marca IT " + suffix;
        eanRice = randomEan13();
        eanBeans = randomEan13();
        rootId = insertId("INSERT INTO categories (name, active, registration_date) "
                + "VALUES (?, true, now()) RETURNING id", "Supermercado IT " + suffix);
        leafId = insertId("INSERT INTO categories (name, active, registration_date, parent_id) "
                + "VALUES (?, true, now(), ?) RETURNING id", "Granos IT " + suffix, rootId);
        storeId = insertId("INSERT INTO stores (name, active, registration_date, category_id) "
                + "VALUES (?, true, now(), ?) RETURNING id", "Tienda IT " + suffix, rootId);
        targetId = insertId("INSERT INTO scrape_targets (store_id, category_id, search_term, unit_base, "
                + "min_content, max_content, active) VALUES (?, ?, 'arroz', 'G', 400, 1000, true) RETURNING id",
                storeId, leafId);
    }

    @AfterEach
    void deleteCatalogData() {
        jdbcTemplate.update("DELETE FROM scraped_offers WHERE store_id = ?", storeId);
        jdbcTemplate.update("DELETE FROM prices WHERE store_id = ?", storeId);
        jdbcTemplate.update("DELETE FROM products WHERE ean IN (?, ?)", gtin14(eanRice), gtin14(eanBeans));
        jdbcTemplate.update("DELETE FROM brands WHERE lower(name) = lower(?)", brandName);
        jdbcTemplate.update("DELETE FROM scrape_targets WHERE id = ?", targetId);
        jdbcTemplate.update("DELETE FROM stores WHERE id = ?", storeId);
        jdbcTemplate.update("DELETE FROM categories WHERE id IN (?, ?)", leafId, rootId);
    }

    @Test
    void procesaSusOfertasYRepetirloNoDuplicaNada() {
        LocalDateTime t0 = LocalDateTime.now().minusHours(3);
        List<Long> offerIds = List.of(
                insertOffer(eanRice, PRICE, "500", "https://tienda.co/arroz-v1", t0),
                insertOffer(eanRice, PRICE, "500", "https://tienda.co/arroz-v2", t0.plusMinutes(1)),
                insertOffer(eanBeans, PRICE, "1000", null, t0.plusMinutes(2)),
                insertOffer(INVALID_EAN, PRICE, "500", null, t0.plusMinutes(3)),
                insertOffer(randomEan13(), PRICE, "2500", null, t0.plusMinutes(4)));

        StagingProcessor.RunSummary first = processor.processOffers(offerIds);

        assertThat(first.count(Result.NEW_PRICE)).isEqualTo(2);
        assertThat(first.count(Result.SAME_PRICE)).isEqualTo(1);
        assertThat(first.count(Result.UNCLASSIFIED)).isEqualTo(2);
        // Arroz: el segundo avistamiento con el mismo valor refresca el periodo, no inserta
        List<Map<String, Object>> rice = pricesOf(eanRice);
        assertThat(rice).hasSize(1);
        assertThat(rice.get(0).get("product_url")).isEqualTo("https://tienda.co/arroz-v2");
        assertThat((Timestamp) rice.get(0).get("last_seen_at"))
                .isAfter((Timestamp) rice.get(0).get("registration_date"));
        assertThat(pricesOf(eanBeans)).hasSize(1);
        assertThat(productField(eanRice, "unit")).isEqualTo("500 g");
        assertThat(productField(eanBeans, "unit")).isEqualTo("1 kg");
        assertThat(productField(eanBeans, "brand_id")).isNotNull();
        assertThat(statusCount("PROCESSED")).isEqualTo(3);
        assertThat(statusCount("UNCLASSIFIED")).isEqualTo(2);
        assertThat(statusCount("PENDING")).isZero();
        assertThat(eventsForOurStore()).hasSize(2);

        StagingProcessor.RunSummary second = processor.processOffers(offerIds);

        assertThat(second.count(Result.SKIPPED)).isEqualTo(offerIds.size());
        assertThat(pricesOf(eanRice)).hasSize(1);
        assertThat(pricesOf(eanBeans)).hasSize(1);
        assertThat(productCount()).isEqualTo(2);
        assertThat(eventsForOurStore()).hasSize(2);
    }

    @Test
    void unPrecioDistintoAbreUnPeriodoNuevoYPublicaSuEvento() {
        LocalDateTime t0 = LocalDateTime.now().minusHours(3);
        processor.processOffers(List.of(insertOffer(eanRice, PRICE, "500", null, t0)));

        processor.processOffers(List.of(insertOffer(eanRice, LOWER_PRICE, "500", null, t0.plusHours(1))));

        List<Map<String, Object>> rice = pricesOf(eanRice);
        assertThat(rice).extracting(row -> (BigDecimal) row.get("value"))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(PRICE, LOWER_PRICE);
        assertThat(rice.get(1).get("source")).isEqualTo("Arroz IT " + suffix);
        assertThat(eventsForOurStore()).hasSize(2);
    }

    @Test
    void dosOfertasDelMismoEanConOtroTamanoNoCreanElProducto() {
        LocalDateTime t0 = LocalDateTime.now().minusHours(3);
        Long first = insertOffer(eanRice, PRICE, "500", null, t0);
        Long second = insertOffer(eanRice, PRICE, "1000", null, t0.plusMinutes(1));

        StagingProcessor.RunSummary summary = processor.processOffers(List.of(first, second));

        // La primera marca a la segunda en su misma transacción; al llegar a ella ya no está PENDING
        assertThat(summary.count(Result.UNCLASSIFIED)).isEqualTo(1);
        assertThat(summary.count(Result.SKIPPED)).isEqualTo(1);
        assertThat(statusCount("UNCLASSIFIED")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "SELECT DISTINCT reason FROM scraped_offers WHERE id IN (?, ?)", String.class, first, second))
                .singleElement().asString()
                .startsWith("Tamaño en conflicto entre ofertas del EAN " + eanRice)
                .contains("1 kg en Tienda IT " + suffix, "500 g en Tienda IT " + suffix);
        assertThat(productCount()).isZero();
        assertThat(eventsForOurStore()).isEmpty();
    }

    // Solo lectura: no procesa nada, verifica el orden (scraped_at, id) de la paginación por clave
    @Test
    void laPaginaSiguienteEmpiezaDespuesDeLaUltimaClaveYDesempataPorId() {
        Long first = insertOffer(eanRice, PRICE, "500", null, KEYSET_BASE);
        Long sameInstant = insertOffer(eanRice, PRICE, "500", null, KEYSET_BASE);
        Long later = insertOffer(eanRice, PRICE, "500", null, KEYSET_BASE.plusSeconds(1));

        List<PendingOfferKey> next = offerRepository.findKeysByStatusAfter(
                OfferStatus.PENDING, KEYSET_BASE, first, PageRequest.ofSize(2));

        assertThat(next).extracting(PendingOfferKey::id).containsExactly(sameInstant, later);
        assertThat(statusCount("PENDING")).isEqualTo(3);
    }

    private Long insertOffer(String ean, BigDecimal price, String netContent, String url, LocalDateTime scrapedAt) {
        String name = ean.equals(eanBeans) ? "Fríjol IT " + suffix : "Arroz IT " + suffix;
        String brand = ean.equals(eanBeans) ? brandName : null;
        return insertId(INSERT_OFFER, storeId, targetId, ean, name, brand, price,
                url, null, new BigDecimal(netContent), Timestamp.valueOf(scrapedAt));
    }

    private List<Map<String, Object>> pricesOf(String ean) {
        return jdbcTemplate.queryForList(PRICES_OF_PRODUCT, gtin14(ean));
    }

    private Object productField(String ean, String column) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM products WHERE ean = ?",
                Object.class, gtin14(ean));
    }

    private int productCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM products WHERE ean IN (?, ?)",
                Integer.class, gtin14(eanRice), gtin14(eanBeans));
    }

    private int statusCount(String status) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM scraped_offers WHERE store_id = ? AND status = ?",
                Integer.class, storeId, status);
    }

    private List<PriceRegisteredEvent> eventsForOurStore() {
        return events.stream(PriceRegisteredEvent.class)
                .filter(event -> event.storeId().equals(storeId))
                .toList();
    }

    private Long insertId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private static String gtin14(String ean13) {
        return "0" + ean13;
    }

    // EAN-13 colombiano aleatorio con dígito verificador válido, para no chocar con el catálogo
    private static String randomEan13() {
        StringBuilder body = new StringBuilder(COLOMBIA_PREFIX);
        while (body.length() < EAN_13_BODY_LENGTH) {
            body.append(ThreadLocalRandom.current().nextInt(10));
        }
        int sum = 0;
        for (int position = 0; position < EAN_13_BODY_LENGTH; position++) {
            int digit = body.charAt(position) - '0';
            sum += position % 2 == 0 ? digit : digit * 3;
        }
        return body.append((10 - sum % 10) % 10).toString();
    }
}
