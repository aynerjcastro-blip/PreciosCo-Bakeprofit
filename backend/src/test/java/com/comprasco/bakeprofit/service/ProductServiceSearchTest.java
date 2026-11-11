package com.comprasco.bakeprofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.comprasco.bakeprofit.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.exception.CategoryNotFoundException;
import com.comprasco.bakeprofit.exception.StoreNotFoundException;
import com.comprasco.bakeprofit.repository.BrandRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;

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

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryService,
                new BrandService(brandRepository), storeService);
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
}
