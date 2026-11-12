package com.comprasco.bakeprofit.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.catalog.entity.Brand;
import com.comprasco.bakeprofit.catalog.repository.BrandRepository;
import com.comprasco.bakeprofit.catalog.service.BrandService;

/**
 * Orden y datos con los que BrandService usa el repositorio. Que dos peticiones
 * simultáneas obtengan la misma marca solo se puede probar contra PostgreSQL:
 * ver BrandConcurrencyIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    private static final String TRIMMED_NAME = "Diana";

    @Mock
    private BrandRepository brandRepository;

    private BrandService brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(brandRepository);
    }

    @Test
    void insertaYBuscaConElNombreRecortado() {
        Brand diana = Brand.named(TRIMMED_NAME);
        when(brandRepository.findByNameMatchingIndex(TRIMMED_NAME)).thenReturn(Optional.of(diana));

        Brand resolved = brandService.resolveByName("  Diana ");

        assertThat(resolved).isSameAs(diana);
        InOrder order = inOrder(brandRepository);
        order.verify(brandRepository).insertIfAbsent(TRIMMED_NAME);
        order.verify(brandRepository).findByNameMatchingIndex(TRIMMED_NAME);
        order.verifyNoMoreInteractions();
    }

    @Test
    void siNoLaEncuentraDespuesDeInsertarFallaConUnErrorClaro() {
        when(brandRepository.findByNameMatchingIndex(TRIMMED_NAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> brandService.resolveByName(TRIMMED_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(TRIMMED_NAME);
    }
}