package com.comprasco.bakeprofit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PagedModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.catalog.dto.BestSavingsGroup;
import com.comprasco.bakeprofit.catalog.dto.ProductResponse;
import com.comprasco.bakeprofit.catalog.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.catalog.dto.ProductSort;
import com.comprasco.bakeprofit.catalog.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.catalog.exception.InvalidSearchFilterException;
import com.comprasco.bakeprofit.catalog.service.ProductService;
import com.comprasco.bakeprofit.store.exception.StoreNotFoundException;

/**
 * Reglas de HU-06b contra PostgreSQL real: el precio vigente, las tiendas activas y el rango
 * se resuelven en SQL, así que una prueba con Mockito no las prueba.
 *
 * <p>
 * Necesita la base de pruebas con scripts/2026-11-scrum140-busqueda-sin-tildes.sql y
 * scripts/2026-11-ht13-ahorro.sql (vista product_savings, HT-13) aplicados.
 * Corre como las demás pruebas de integración (ver BrandConcurrencyIntegrationTest):
 * </p>
 *
 * <pre>
 * ./mvnw test "-Dgroups=integration" "-Dsurefire.excludedGroups="
 * </pre>
 *
 * <p>
 * Cada prueba corre en una transacción que se revierte al final: sus categorías, tiendas,
 * productos y precios nunca quedan en la base. Los nombres llevan un sufijo único y las
 * consultas filtran por la categoría de la prueba o por ese sufijo, así que los datos
 * ajenos no cambian los resultados.
 * </p>
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest
@Transactional
class ProductSearchIntegrationTest {

    private static final int PAGE_SIZE = 20;
    private static final LocalDateTime LAST_MONTH = LocalDateTime.now().minusMonths(1);
    private static final LocalDateTime TODAY = LocalDateTime.now().withNano(0);

    private static final String INSERT_CATEGORY =
            "INSERT INTO categories (name, active, registration_date, parent_id) VALUES (?, true, now(), ?) RETURNING id";
    private static final String INSERT_STORE =
            "INSERT INTO stores (name, active, registration_date, category_id) VALUES (?, ?, now(), ?) RETURNING id";
    private static final String INSERT_PRODUCT =
            "INSERT INTO products (name, active, registration_date, category_id) VALUES (?, ?, now(), ?) RETURNING id";
    private static final String INSERT_PRICE =
            "INSERT INTO prices (value, registration_date, last_seen_at, source, product_id, store_id) "
                    + "VALUES (?, ?, ?, 'prueba SCRUM-140', ?, ?)";

    @Autowired
    private ProductService productService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String suffix;
    private Long rootCategoryId;
    private Long leafCategoryId;
    private Long storeA;
    private Long storeB;
    private Long inactiveStore;

    private Long cafe;
    private Long arroz;
    private Long azucar;
    private Long sal;
    private Long lenteja;

    /*
     * Catálogo de la prueba (precios vigentes):
     *   Arroz Blanco  A 5000 (antes 3000)
     *   Azúcar        solo en la tienda inactiva, 1000
     *   Café Molido   A 3000, B 6000
     *   Lenteja       A 7000 (empata en fecha con un 2000 anterior: gana el id mayor)
     *   Sal           sin precio
     *   Café Viejo    producto inactivo, A 1000
     */
    @BeforeEach
    void createCatalog() {
        suffix = UUID.randomUUID().toString().replace("-", "");
        rootCategoryId = insertId(INSERT_CATEGORY, "Dominio IT " + suffix, null);
        leafCategoryId = insertId(INSERT_CATEGORY, "Granos IT " + suffix, rootCategoryId);
        storeA = insertId(INSERT_STORE, "Tienda A " + suffix, true, rootCategoryId);
        storeB = insertId(INSERT_STORE, "Tienda B " + suffix, true, rootCategoryId);
        inactiveStore = insertId(INSERT_STORE, "Tienda inactiva " + suffix, false, rootCategoryId);

        arroz = product("Arroz Blanco", true);
        azucar = product("Azúcar", true);
        cafe = product("Café Molido", true);
        lenteja = product("Lenteja", true);
        sal = product("Sal", true);
        Long cafeViejo = product("Café Viejo", false);

        price(arroz, storeA, "3000", LAST_MONTH);
        price(arroz, storeA, "5000", TODAY);
        price(azucar, inactiveStore, "1000", TODAY);
        price(cafe, storeA, "3000", TODAY);
        price(cafe, storeB, "6000", TODAY);
        price(lenteja, storeA, "2000", TODAY);
        price(lenteja, storeA, "7000", TODAY);
        price(cafeViejo, storeA, "1000", TODAY);
    }

    @Test
    void sinTiendaNiRangoSeComportaComoHu06a() {
        assertThat(idsOf(inCategory(List.of(), null, null)))
                .containsExactly(arroz, azucar, cafe, lenteja, sal);
    }

    @Test
    void elNombreNoDistingueTildesNiMayusculas() {
        assertThat(idsOf(byName("CAFE " + suffix))).containsExactly(cafe);
        assertThat(idsOf(byName("azucar " + suffix))).containsExactly(azucar);
        assertThat(idsOf(byName("molido café " + suffix))).containsExactly(cafe);
    }

    @Test
    void unaTiendaDejaSoloLosProductosConPrecioEnElla() {
        assertThat(idsOf(inCategory(List.of(storeB), null, null))).containsExactly(cafe);
    }

    @Test
    void baratoEnUnaTiendaYCaroEnOtraConPrecioMaximo() {
        BigDecimal max = new BigDecimal("4000");

        assertThat(idsOf(inCategory(List.of(storeA), null, max))).containsExactly(cafe);
        assertThat(idsOf(inCategory(List.of(storeB), null, max))).isEmpty();
        assertThat(idsOf(inCategory(List.of(), null, max))).containsExactly(cafe);
    }

    @Test
    void elRangoSeEvaluaSobreElPrecioVigenteNoSobreElHistorial() {
        assertThat(idsOf(inCategory(List.of(storeA), null, new BigDecimal("3500")))).containsExactly(cafe);
        assertThat(idsOf(inCategory(List.of(), new BigDecimal("4500"), new BigDecimal("5500"))))
                .containsExactly(arroz);
    }

    @Test
    void conFechasIgualesElPrecioVigenteEsElDeIdMayor() {
        assertThat(idsOf(inCategory(List.of(storeA), new BigDecimal("6000"), null))).containsExactly(lenteja);
        assertThat(idsOf(inCategory(List.of(storeA), null, new BigDecimal("2500")))).isEmpty();
    }

    @Test
    void sinTiendaSeleccionadaSoloCuentanLasActivas() {
        assertThat(idsOf(inCategory(List.of(), BigDecimal.ZERO, null))).containsExactly(arroz, cafe, lenteja);
    }

    @Test
    void losLimitesSonInclusivos() {
        BigDecimal exact = new BigDecimal("3000");

        assertThat(idsOf(inCategory(List.of(storeA), exact, exact))).containsExactly(cafe);
    }

    @Test
    void combinadoConNombreYCategoriaCumpleTodosLosFiltros() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of(
                "arroz", leafCategoryId, List.of(storeA), null, new BigDecimal("6000"));

        assertThat(idsOf(productService.search(criteria, ProductSort.NAME, 0, PAGE_SIZE))).containsExactly(arroz);
    }

    @Test
    void unaTiendaInactivaEnElFiltroResponde404() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, leafCategoryId, List.of(inactiveStore), null, null);

        assertThatThrownBy(() -> productService.search(criteria, ProductSort.NAME, 0, PAGE_SIZE))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    void lasPaginasEstanCompletasYElTotalEsExactoConFiltroDeNombre() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of("a " + suffix, leafCategoryId, List.of(), null, null);

        PagedModel<ProductResponse> first = productService.search(criteria, ProductSort.NAME, 0, 2);
        PagedModel<ProductResponse> second = productService.search(criteria, ProductSort.NAME, 1, 2);

        // "a" aparece en Arroz Blanco, Azúcar, Café Molido, Lenteja y Sal: los cinco
        assertThat(first.getMetadata().totalElements()).isEqualTo(5);
        assertThat(first.getMetadata().totalPages()).isEqualTo(3);
        assertThat(idsOf(first)).containsExactly(arroz, azucar);
        assertThat(idsOf(second)).containsExactly(cafe, lenteja);
    }

    // ===================== HT-13: sección y orden por mayor ahorro =====================

    @Test
    void ordenaPorMayorAhorroYLosDeUnSoloPrecioOSinPrecioVanAlFinal() {
        // Leche 4800/6000 = 20 %; Pan 5000/5200 = 3,8 %; Café 3000/6000 = 50 %
        Long leche = product("Leche", true);
        Long pan = product("Pan", true);
        price(leche, storeA, "4800", TODAY);
        price(leche, storeB, "6000", TODAY);
        price(pan, storeA, "5000", TODAY);
        price(pan, storeB, "5200", TODAY);

        // Ahorro 0 desempata por nombre: Arroz y Lenteja (un precio), Azúcar y Sal (sin precio activo)
        assertThat(idsOf(bySavings(List.of(), 0, PAGE_SIZE)))
                .containsExactly(cafe, leche, pan, arroz, azucar, lenteja, sal);
    }

    @Test
    void unaTiendaInactivaNoEntraEnElCalculoDelAhorro() {
        // Con la inactiva sería 5000/10000 = 50 %; sin ella tiene un solo precio y ahorro 0
        Long aceite = product("Aceite", true);
        price(aceite, storeA, "5000", TODAY);
        price(aceite, inactiveStore, "10000", TODAY);

        assertThat(savingsOf(aceite)).isEqualByComparingTo("0");
        assertThat(idsOf(bySavings(List.of(), 0, PAGE_SIZE))).containsExactly(cafe, aceite, arroz, azucar, lenteja, sal);
    }

    @Test
    void conTiendasFiltradasElAhorroSeCalculaConTodasLasActivas() {
        // Solo la tienda A tiene precio de Café en el filtro, pero su ahorro (50 %) usa también la B
        assertThat(idsOf(bySavings(List.of(storeA), 0, PAGE_SIZE))).containsExactly(cafe, arroz, lenteja);
    }

    @Test
    void elOrdenPorAhorroSeMantieneEntrePaginas() {
        List<Long> all = idsOf(bySavings(List.of(), 0, PAGE_SIZE));
        List<Long> first = idsOf(bySavings(List.of(), 0, 3));
        List<Long> second = idsOf(bySavings(List.of(), 1, 3));

        assertThat(first).containsExactlyElementsOf(all.subList(0, 3));
        assertThat(second).containsExactlyElementsOf(all.subList(3, all.size()));
    }

    @Test
    void laSeccionDejaLosProductosConPrecioEnUnaTiendaActivaDeSuDominio() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, leafCategoryId, rootCategoryId, List.of(), null, null);

        // Azúcar solo tiene precio en la tienda inactiva y Sal no tiene precio: no entran
        assertThat(idsOf(productService.search(criteria, ProductSort.SAVINGS, 0, PAGE_SIZE)))
                .containsExactly(cafe, arroz, lenteja);
    }

    @Test
    void unaSeccionSinTiendasRespondeVacioSinError() {
        Long otherRoot = insertId(INSERT_CATEGORY, "Otro dominio IT " + suffix, null);
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, leafCategoryId, otherRoot, List.of(), null, null);

        assertThat(productService.search(criteria, ProductSort.SAVINGS, 0, PAGE_SIZE).getContent()).isEmpty();
    }

    @Test
    void unaSeccionInexistenteOQueNoEsRaizResponde404() {
        ProductSearchCriteria missing = ProductSearchCriteria.of(null, null, Long.MAX_VALUE, List.of(), null, null);
        ProductSearchCriteria leaf = ProductSearchCriteria.of(null, null, leafCategoryId, List.of(), null, null);

        assertThatThrownBy(() -> productService.search(missing, ProductSort.SAVINGS, 0, PAGE_SIZE))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> productService.search(leaf, ProductSort.SAVINGS, 0, PAGE_SIZE))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> productService.bestSavings(Long.MAX_VALUE, 5))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void sinFiltrosCadaSubcategoriaMuestraComoMaximoNPorAhorro() {
        List<BestSavingsGroup> groups = productService.bestSavings(rootCategoryId, 2);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).categoryId()).isEqualTo(leafCategoryId);
        assertThat(groups.get(0).sectionId()).isEqualTo(rootCategoryId);
        // Café (50 %) y luego el primero por nombre de ahorro 0; Azúcar y Sal no tienen precio activo
        assertThat(groups.get(0).products()).extracting(item -> item.id()).containsExactly(cafe, arroz);
        assertThat(productService.bestSavings(rootCategoryId, 5).get(0).products())
                .extracting(item -> item.id()).containsExactly(cafe, arroz, lenteja);
    }

    @Test
    void unLimiteFueraDeRangoResponde400() {
        assertThatThrownBy(() -> productService.bestSavings(rootCategoryId, 0))
                .isInstanceOf(InvalidSearchFilterException.class);
        assertThatThrownBy(() -> productService.bestSavings(rootCategoryId, ProductService.MAX_BEST_SAVINGS_LIMIT + 1))
                .isInstanceOf(InvalidSearchFilterException.class);
    }

    private PagedModel<ProductResponse> bySavings(List<Long> storeIds, int page, int size) {
        return productService.search(
                ProductSearchCriteria.of(null, leafCategoryId, storeIds, null, null), ProductSort.SAVINGS, page, size);
    }

    private BigDecimal savingsOf(Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT savings_pct FROM product_savings WHERE product_id = ?", BigDecimal.class, productId);
    }

    private PagedModel<ProductResponse> inCategory(List<Long> storeIds, BigDecimal min, BigDecimal max) {
        return productService.search(
                ProductSearchCriteria.of(null, leafCategoryId, storeIds, min, max), ProductSort.NAME, 0, PAGE_SIZE);
    }

    private PagedModel<ProductResponse> byName(String name) {
        return productService.search(ProductSearchCriteria.of(name, null, List.of(), null, null), ProductSort.NAME, 0, PAGE_SIZE);
    }

    private static List<Long> idsOf(PagedModel<ProductResponse> page) {
        return page.getContent().stream().map(ProductResponse::id).toList();
    }

    private Long product(String name, boolean active) {
        return insertId(INSERT_PRODUCT, name + " " + suffix, active, leafCategoryId);
    }

    private void price(Long productId, Long storeId, String value, LocalDateTime start) {
        Timestamp startedAt = Timestamp.valueOf(start);
        jdbcTemplate.update(INSERT_PRICE, new BigDecimal(value), startedAt, startedAt, productId, storeId);
    }

    private Long insertId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }
}
