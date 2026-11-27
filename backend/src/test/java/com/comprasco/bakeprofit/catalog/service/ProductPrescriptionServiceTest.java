package com.comprasco.bakeprofit.catalog.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.comprasco.bakeprofit.catalog.event.ProductPrescriptionChangedEvent;
import com.comprasco.bakeprofit.catalog.exception.ProductNotFoundException;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;

/** HU-39: marca de fórmula médica desde administración. */
@ExtendWith(MockitoExtension.class)
class ProductPrescriptionServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProductPrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new ProductPrescriptionService(productRepository, eventPublisher);
    }

    @Test
    void marksProductAndNotifiesSoTheAiCacheIsDiscarded() {
        when(productRepository.updateRequiresPrescription(5L, true)).thenReturn(1);

        service.setRequiresPrescription(5L, true);

        verify(productRepository).updateRequiresPrescription(5L, true);
        verify(eventPublisher).publishEvent(new ProductPrescriptionChangedEvent(5L, true));
    }

    @Test
    void releasesTheMark() {
        when(productRepository.updateRequiresPrescription(5L, false)).thenReturn(1);

        service.setRequiresPrescription(5L, false);

        verify(eventPublisher).publishEvent(new ProductPrescriptionChangedEvent(5L, false));
    }

    @Test
    void unknownProductIsNotFound_andPublishesNothing() {
        when(productRepository.updateRequiresPrescription(99L, true)).thenReturn(0);

        assertThatThrownBy(() -> service.setRequiresPrescription(99L, true))
                .isInstanceOf(ProductNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
