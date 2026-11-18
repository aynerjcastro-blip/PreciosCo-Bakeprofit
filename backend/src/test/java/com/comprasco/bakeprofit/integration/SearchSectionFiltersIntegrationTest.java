package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.comprasco.bakeprofit.catalog.controller.CategoryController;
import com.comprasco.bakeprofit.catalog.dto.CategoryResponse;
import com.comprasco.bakeprofit.catalog.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.store.controller.StoreController;
import com.comprasco.bakeprofit.store.dto.StoreResponse;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest(properties = "preciosco.staging.enabled=false")
@Transactional
class SearchSectionFiltersIntegrationTest {
    private static final String INSERT_CATEGORY = """
            INSERT INTO categories(name, active, registration_date, parent_id)
            VALUES (?, ?, now(), ?) RETURNING id
            """;
    private static final String INSERT_STORE = """
            INSERT INTO stores(name, active, registration_date, category_id)
            VALUES (?, ?, now(), ?) RETURNING id
            """;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CategoryController categories;
    @Autowired private StoreController stores;
    private Long firstSection;
    private Long secondSection;
    private Long inactiveSection;
    private Long firstCategory;
    private Long secondCategory;
    private Long inactiveCategory;
    private Long firstStore;
    private Long secondStore;

    @BeforeEach
    void prepareSections() {
        String suffix = UUID.randomUUID().toString();
        firstSection = jdbc.queryForObject(INSERT_CATEGORY, Long.class, "Dominio A " + suffix, true, null);
        secondSection = jdbc.queryForObject(INSERT_CATEGORY, Long.class, "Dominio B " + suffix, true, null);
        inactiveSection = jdbc.queryForObject(INSERT_CATEGORY, Long.class, "Dominio inactivo " + suffix, false, null);
        firstCategory = jdbc.queryForObject(INSERT_CATEGORY, Long.class, "Hoja A " + suffix, true, firstSection);
        secondCategory = jdbc.queryForObject(INSERT_CATEGORY, Long.class, "Hoja B " + suffix, true, secondSection);
        inactiveCategory = jdbc.queryForObject(INSERT_CATEGORY, Long.class, "Hoja inactiva " + suffix, false, firstSection);
        firstStore = jdbc.queryForObject(INSERT_STORE, Long.class, "Tienda A " + suffix, true, firstSection);
        secondStore = jdbc.queryForObject(INSERT_STORE, Long.class, "Tienda B " + suffix, true, secondSection);
        jdbc.queryForObject(INSERT_STORE, Long.class, "Tienda inactiva " + suffix, false, firstSection);
    }

    @Test
    void cadaSeccionDevuelveSoloSusCategoriasYTiendasActivas() {
        assertThat(categories.findActive(firstSection).getBody()).extracting(CategoryResponse::id)
                .containsExactly(firstCategory);
        assertThat(categories.findActive(secondSection).getBody()).extracting(CategoryResponse::id)
                .containsExactly(secondCategory);
        assertThat(stores.findActive(firstSection).getBody()).extracting(StoreResponse::id)
                .containsExactly(firstStore);
        assertThat(stores.findActive(secondSection).getBody()).extracting(StoreResponse::id)
                .containsExactly(secondStore);
    }

    @Test
    void sinSeccionConservaElContratoGlobal() {
        assertThat(categories.findActive(null).getBody()).extracting(CategoryResponse::id)
                .contains(firstCategory, secondCategory).doesNotContain(inactiveCategory, firstSection);
        assertThat(stores.findActive(null).getBody()).extracting(StoreResponse::id)
                .contains(firstStore, secondStore);
    }

    @Test
    void listadoDeSeccionesNoIncluyeHojasNiRaicesInactivas() {
        assertThat(categories.findActiveRoots().getBody()).extracting(CategoryResponse::id)
                .contains(firstSection, secondSection).doesNotContain(firstCategory, inactiveSection);
    }

    @Test
    void raizInactivaHojaOIdInexistenteNoAbrenUnaBusquedaGlobal() {
        for (Long invalidSection : java.util.List.of(inactiveSection, firstCategory, Long.MAX_VALUE)) {
            assertThatThrownBy(() -> categories.findActive(invalidSection))
                    .isInstanceOf(CategoryNotFoundException.class);
            assertThatThrownBy(() -> stores.findActive(invalidSection))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }
}
