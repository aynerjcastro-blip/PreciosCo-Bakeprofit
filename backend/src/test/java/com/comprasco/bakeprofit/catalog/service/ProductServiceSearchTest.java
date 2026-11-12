package com.comprasco.bakeprofit.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import com.comprasco.bakeprofit.catalog.dto.ProductResponse;
import com.comprasco.bakeprofit.catalog.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.catalog.repository.BrandRepository;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.store.exception.StoreNotFoundException;
import com.comprasco.bakeprofit.price.dto.ProductImage;
import com.comprasco.bakeprofit.price.repository.PriceRepository;
import com.comprasco.bakeprofit.store.service.StoreService;

/**
 * Orquestación de la búsqueda (HU-06a, HU-06b): validaciones antes de consultar y
 * paginación acotada. Las reglas de los filtros viven en SQL y las prueba
 * ProductSearchIntegrationTest contra PostgreSQL.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceSearchTest {

    private static final Long CATEGORY_ID = 3L;
    private static final Long STORE_ID = 8L;

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private StoreService storeService;
    @Mock
    private PriceRepository priceRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryService,
                new BrandService(brandRepository), storeService, priceRepository);
    }

    @SuppressWarnings("unchecked")
    private Pageable searchAndCapturePageable(ProductSearchCriteria criteria, int page, int size) {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.<Product>empty());
        productService.search(criteria, page, size);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageable.capture());
        return pageable.getValue();
    }

    @Test
    void categoriaInexistenteResponde404SinConsultarProductos() {
        when(categoryService.findById(CATEGORY_ID)).thenThrow(new CategoryNotFoundException("no existe"));
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, CATEGORY_ID, null, null, null);

        assertThatThrownBy(() -> productService.search(criteria, 0, 20))
                .isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(productRepository);
    }

    @Test
    void tiendaNoDisponibleResponde404SinConsultarProductos() {
        doThrow(new StoreNotFoundException("Tienda no encontrada o inactiva: 8"))
                .when(storeService).requireActive(Set.of(STORE_ID));
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, null, List.of(STORE_ID), null, null);

        assertThatThrownBy(() -> productService.search(criteria, 0, 20))
                .isInstanceOf(StoreNotFoundException.class);
        verifyNoInteractions(productRepository);
    }

    @Test
    void lasPaginasSeOrdenanPorNombreYLuegoPorId() {
        Pageable pageable = searchAndCapturePageable(ProductSearchCriteria.of("arroz", null, null, null, null), 2, 20);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
    }

    @Test
    void elTamanoSeAcotaEntreUnoYElMaximo() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, null, null, null, null);

        assertThat(searchAndCapturePageable(criteria, 0, 500).getPageSize()).isEqualTo(ProductService.MAX_PAGE_SIZE);
    }

    @Test
    void unaPaginaNegativaEsLaPrimera() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of(null, null, null, null, null);

        Pageable pageable = searchAndCapturePageable(criteria, -3, 0);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(1);
    }

    private Product productWithId(Long id, String name) {
        Category category = new Category();
        category.setName("Aceites");
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setCategory(category);
        return product;
    }

    @SuppressWarnings("unchecked")
    private PagedModel<ProductResponse> searchPageOf(Product... products) {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(products)));
        return productService.search(ProductSearchCriteria.of(null, null, null, null, null), 0, 20);
    }

    @Test
    void cadaProductoRecibeLaFotoDeSuPrecioVigenteMasBajo() {
        // El repositorio entrega las fotos del precio más bajo primero: la primera de cada producto gana
        when(priceRepository.findCurrentImagesByProductIds(any())).thenReturn(List.of(
                new ProductImage(1L, "https://tienda-a/aceite-barato.png"),
                new ProductImage(2L, "https://tienda-b/arroz.png"),
                new ProductImage(1L, "https://tienda-b/aceite-caro.png")));

        PagedModel<ProductResponse> result = searchPageOf(productWithId(1L, "Aceite"), productWithId(2L, "Arroz"));

        assertThat(result.getContent()).extracting(ProductResponse::imageUrl)
                .containsExactly("https://tienda-a/aceite-barato.png", "https://tienda-b/arroz.png");
    }

    @Test
    void unProductoSinFotoQuedaConImageUrlNulo() {
        when(priceRepository.findCurrentImagesByProductIds(any()))
                .thenReturn(List.of(new ProductImage(1L, "https://tienda-a/aceite.png")));

        PagedModel<ProductResponse> result = searchPageOf(productWithId(1L, "Aceite"), productWithId(2L, "Arroz"));

        assertThat(result.getContent()).extracting(ProductResponse::imageUrl)
                .containsExactly("https://tienda-a/aceite.png", null);
    }

    @Test
    void lasFotosSePidenEnUnaSolaConsultaPorPagina() {
        when(priceRepository.findCurrentImagesByProductIds(any())).thenReturn(List.of());

        searchPageOf(productWithId(1L, "Aceite"), productWithId(2L, "Arroz"), productWithId(3L, "Sal"));

        verify(priceRepository).findCurrentImagesByProductIds(List.of(1L, 2L, 3L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unaPaginaVaciaNoConsultaFotos() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.<Product>empty());

        productService.search(ProductSearchCriteria.of(null, null, null, null, null), 0, 20);

        verify(priceRepository, never()).findCurrentImagesByProductIds(any());
    }
}