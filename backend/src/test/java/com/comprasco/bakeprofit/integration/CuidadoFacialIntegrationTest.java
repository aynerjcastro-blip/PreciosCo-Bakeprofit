package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.comprasco.bakeprofit.catalog.dto.ProductResponse;
import com.comprasco.bakeprofit.catalog.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.catalog.dto.ProductSort;
import com.comprasco.bakeprofit.catalog.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.catalog.service.ProductService;
import com.comprasco.bakeprofit.price.controller.PriceController;
import com.comprasco.bakeprofit.price.dto.PriceCompareResponse;
import com.comprasco.bakeprofit.staging.service.StagingProcessor;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest(properties = "preciosco.staging.enabled=false")
class Hu30CuidadoFacialIntegrationTest {

    private static final LocalDateTime KEYSET_BASE = LocalDateTime.of(2001, 2, 1, 0, 0);

    @Autowired
    private StagingProcessor processor;
    @Autowired
    private ProductService productService;
    @Autowired
    private PriceController priceController;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String suffix;
    private Long rootId;
    private Long leafId;
    private Long storeAId;
    private Long storeBId;
    private Long targetAId;
    private Long targetBId;
    private String sharedEan13;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        sharedEan13 = randomColombiaEan13();

        rootId = insertId(
                "INSERT INTO categories (name, active, registration_date) VALUES (?, true, now()) RETURNING id",
                "Cuidado facial IT " + suffix);
        leafId = insertId(
                "INSERT INTO categories (name, active, registration_date, parent_id) VALUES (?, true, now(), ?) RETURNING id",
                "Serums IT " + suffix, rootId);

        storeAId = insertId(
                "INSERT INTO stores (name, active, registration_date, category_id) VALUES (?, true, now(), ?) RETURNING id",
                "Cutis IT " + suffix, rootId);
        storeBId = insertId(
                "INSERT INTO stores (name, active, registration_date, category_id) VALUES (?, true, now(), ?) RETURNING id",
                "Bellapiel IT " + suffix, rootId);

        targetAId = insertId(
                "INSERT INTO scrape_targets (store_id, category_id, search_term, unit_base, min_content, max_content, active) "
                        + "VALUES (?, ?, 'serum', 'ML', 10, 200, true) RETURNING id",
                storeAId, leafId);
        targetBId = insertId(
                "INSERT INTO scrape_targets (store_id, category_id, search_term, unit_base, min_content, max_content, active) "
                        + "VALUES (?, ?, 'serum', 'ML', 10, 200, true) RETURNING id",
                storeBId, leafId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM scraped_offers WHERE store_id IN (?, ?)", storeAId, storeBId);
        jdbcTemplate.update("DELETE FROM prices WHERE store_id IN (?, ?)", storeAId, storeBId);
        jdbcTemplate.update("DELETE FROM store_product_links WHERE store_id IN (?, ?)", storeAId, storeBId);
        jdbcTemplate.update("DELETE FROM store_sku_ean_map WHERE store_id IN (?, ?)", storeAId, storeBId);
        jdbcTemplate.update("DELETE FROM products WHERE category_id = ?", leafId);
        jdbcTemplate.update("DELETE FROM scrape_targets WHERE id IN (?, ?)", targetAId, targetBId);
        jdbcTemplate.update("DELETE FROM stores WHERE id IN (?, ?)", storeAId, storeBId);
        jdbcTemplate.update("DELETE FROM categories WHERE id IN (?, ?)", leafId, rootId);
    }

    @Test
    void comparaProductoEnDosTiendasDeCuidadoFacialYRechazaSeccionInexistente() {
        Long offerA = insertOffer(storeAId, targetAId, sharedEan13, "Serum Facial " + suffix,
                new BigDecimal("45000.00"), KEYSET_BASE);
        Long offerB = insertOffer(storeBId, targetBId, sharedEan13, "Serum Facial " + suffix,
                new BigDecimal("38000.00"), KEYSET_BASE.plusMinutes(1));

        processor.processOffers(List.of(offerA, offerB));

        ProductSearchCriteria criteria = ProductSearchCriteria.of(
                null, null, rootId, null, null, null);
        List<ProductResponse> found = productService.search(criteria, ProductSort.SAVINGS, 0, 20).getContent();
        assertThat(found).hasSize(1);

        Long productId = found.get(0).id();
        List<PriceCompareResponse> comparison = priceController.compare(productId).getBody();
        assertThat(comparison).isNotNull();
        assertThat(comparison).hasSize(2);

        assertThatThrownBy(() -> productService.search(
                ProductSearchCriteria.of(null, null, 999_999_999L, null, null, null),
                ProductSort.SAVINGS, 0, 20))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    private Long insertOffer(Long storeId, Long targetId, String ean, String name,
            BigDecimal price, LocalDateTime scrapedAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO scraped_offers (store_id, scrape_target_id, ean, name, brand_name, price,
                                            product_url, image_url, net_content, unit_base, scraped_at, status)
                VALUES (?, ?, ?, ?, ?, ?, 'https://ejemplo.co/p', 'https://ejemplo.co/img.png', 30, 'ML', ?, 'PENDING')
                RETURNING id
                """, Long.class, storeId, targetId, ean, name, "Marca IT " + suffix, price, scrapedAt);
    }

    private Long insertId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private static String randomColombiaEan13() {
        StringBuilder sb = new StringBuilder("770");
        for (int i = 0; i < 9; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        String body12 = sb.toString();
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = body12.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return body12 + checkDigit;
    }
}