package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.price.dto.PriceCompareResponse;
import com.comprasco.bakeprofit.price.repository.PriceRepository;
import com.comprasco.bakeprofit.price.service.PriceService;

/**
 * El comparador y el precio más bajo de Seguidos ignoran las tiendas inactivas, igual que la
 * búsqueda (HU-06b). Corre como ProductSearchIntegrationTest y revierte sus datos.
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest
@Transactional
class PriceCompareIntegrationTest {

    private static final String INSERT_CATEGORY =
            "INSERT INTO categories (name, active, registration_date, parent_id) VALUES (?, true, now(), ?) RETURNING id";
    private static final String INSERT_STORE =
            "INSERT INTO stores (name, active, registration_date, category_id) VALUES (?, ?, now(), ?) RETURNING id";
    private static final String INSERT_PRODUCT =
            "INSERT INTO products (name, active, registration_date, category_id) VALUES (?, true, now(), ?) RETURNING id";
    private static final String INSERT_PRICE =
            "INSERT INTO prices (value, registration_date, last_seen_at, source, product_id, store_id) "
                    + "VALUES (?, now(), now(), 'prueba comparador', ?, ?)";

    @Autowired
    private PriceService priceService;
    @Autowired
    private PriceRepository priceRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long productId;
    private Long activeStoreId;

    @BeforeEach
    void createPrices() {
        String suffix = UUID.randomUUID().toString();
        Long root = insertId(INSERT_CATEGORY, "Dominio comparador " + suffix, null);
        Long leaf = insertId(INSERT_CATEGORY, "Hoja comparador " + suffix, root);
        activeStoreId = insertId(INSERT_STORE, "Activa " + suffix, true, root);
        Long inactiveStoreId = insertId(INSERT_STORE, "Inactiva " + suffix, false, root);
        productId = insertId(INSERT_PRODUCT, "Producto comparador " + suffix, leaf);

        jdbcTemplate.update(INSERT_PRICE, new BigDecimal("4000"), productId, activeStoreId);
        jdbcTemplate.update(INSERT_PRICE, new BigDecimal("1500"), productId, inactiveStoreId);
    }

    @Test
    void elComparadorNoMuestraTiendasInactivas() {
        assertThat(priceService.compareByProduct(productId))
                .extracting(PriceCompareResponse::storeId)
                .containsExactly(activeStoreId);
    }

    @Test
    void elPrecioMasBajoIgnoraLasTiendasInactivas() {
        assertThat(priceRepository.findMinPriceByProduct(productId)).isEqualByComparingTo("4000");
    }

    private Long insertId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }
}
