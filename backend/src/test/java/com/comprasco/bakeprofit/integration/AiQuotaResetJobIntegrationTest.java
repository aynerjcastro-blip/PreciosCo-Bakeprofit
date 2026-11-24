package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.repository.UserRepository;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest(classes = AiQuotaResetJobIntegrationTest.PersistenceConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class AiQuotaResetJobIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class PersistenceConfiguration {
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reiniciaSoloLaCuotaVencidaYAvanzaUnMesDesdeSuFechaAnterior() {
        jdbcTemplate.execute("SET LOCAL TIME ZONE 'UTC'");
        OffsetDateTime previousReset = OffsetDateTime.parse("2000-01-31T12:00:00Z");
        OffsetDateTime now = OffsetDateTime.parse("2000-02-01T12:00:00Z");
        OffsetDateTime futureReset = now.plusDays(7);
        Long dueUserId = insertUser(previousReset);
        Long futureUserId = insertUser(futureReset);

        assertThat(userRepository.resetDueAiQuotas(now)).isEqualTo(1);

        assertThat(userRepository.findById(dueUserId)).hasValueSatisfying(user -> {
            assertThat(user.getAiQueriesUsed()).isZero();
            assertThat(user.getAiQueriesResetAt()).isEqualTo(previousReset.plusMonths(1));
        });
        assertThat(userRepository.findById(futureUserId)).hasValueSatisfying(user -> {
            assertThat(user.getAiQueriesUsed()).isEqualTo(3);
            assertThat(user.getAiQueriesResetAt()).isEqualTo(futureReset);
        });
        assertThat(userRepository.resetDueAiQuotas(now)).isZero();
    }

    private Long insertUser(OffsetDateTime resetAt) {
        return jdbcTemplate.queryForObject("""
            INSERT INTO users (name, email, role, active, password, registration_date,
                               failed_attempts, auth_provider, ai_queries_used, ai_queries_reset_at)
            VALUES ('Cuota IA IT', ?, 'USER', true, 'fixture', ?, 0, 'LOCAL', 3, ?)
            RETURNING id
            """, Long.class, "ai-quota-" + UUID.randomUUID() + "@example.test", resetAt.toLocalDateTime(), resetAt);
    }
}
