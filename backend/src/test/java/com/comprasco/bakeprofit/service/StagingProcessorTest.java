package com.comprasco.bakeprofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import com.comprasco.bakeprofit.entity.OfferStatus;
import com.comprasco.bakeprofit.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.repository.PendingOfferKey;
import com.comprasco.bakeprofit.repository.ScrapedOfferRepository;
import com.comprasco.bakeprofit.service.OfferIngestionService.Outcome;
import com.comprasco.bakeprofit.service.OfferIngestionService.Result;

@ExtendWith(MockitoExtension.class)
class StagingProcessorTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 7, 6, 0);
    private static final PendingOfferKey FIRST = new PendingOfferKey(1L, T0);
    private static final PendingOfferKey SECOND = new PendingOfferKey(2L, T0);
    private static final PendingOfferKey THIRD = new PendingOfferKey(3L, T0.plusMinutes(1));
    private static final PriceRegisteredEvent EVENT = new PriceRegisteredEvent(55L, 7L, 1L);

    @Mock
    private ScrapedOfferRepository offerRepository;
    @Mock
    private OfferIngestionService ingestionService;
    @Mock
    private OfferFailureRecorder failureRecorder;
    @Mock
    private StagingRetention retention;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StagingProcessor processor;

    @BeforeEach
    void setUp() {
        // batch-size = 2: tres ofertas obligan a pedir una segunda página. Lenient porque
        // processOffers no pagina
        processor = new StagingProcessor(offerRepository, ingestionService, failureRecorder, retention,
                eventPublisher, StagingFixtures.properties());
        lenient().when(offerRepository.findKeysByStatus(eq(OfferStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(FIRST, SECOND));
        lenient().when(offerRepository.findKeysByStatusAfter(eq(OfferStatus.PENDING), eq(T0), eq(2L), any(Pageable.class)))
                .thenReturn(List.of(THIRD));
        lenient().when(offerRepository.findKeysByStatusAfter(eq(OfferStatus.PENDING), eq(T0.plusMinutes(1)), eq(3L),
                any(Pageable.class))).thenReturn(List.of());
    }

    @Test
    void unaExcepcionEnUnaOfertaNoDetieneElLoteYLaMarcaEnOtraTransaccion() {
        RuntimeException failure = new IllegalStateException("BD caída");
        when(ingestionService.process(1L)).thenReturn(new Outcome(Result.NEW_PRICE, EVENT));
        when(ingestionService.process(2L)).thenThrow(failure);
        when(ingestionService.process(3L)).thenReturn(new Outcome(Result.SAME_PRICE, null));

        StagingProcessor.RunSummary summary = processor.runOnce();

        assertThat(summary.count(Result.NEW_PRICE)).isEqualTo(1);
        assertThat(summary.count(Result.FAILED)).isEqualTo(1);
        assertThat(summary.count(Result.SAME_PRICE)).isEqualTo(1);
        verify(failureRecorder).markFailed(2L, failure);
        verify(ingestionService).process(3L);
    }

    @Test
    void publicaElEventoDespuesDeProcesarLaOfertaYAplicaLaRetencionAlFinal() {
        when(ingestionService.process(1L)).thenReturn(new Outcome(Result.NEW_PRICE, EVENT));
        when(ingestionService.process(2L)).thenReturn(new Outcome(Result.UNCLASSIFIED, null));
        when(ingestionService.process(3L)).thenReturn(new Outcome(Result.SAME_PRICE, null));

        processor.runOnce();

        InOrder order = inOrder(ingestionService, eventPublisher, retention);
        order.verify(ingestionService).process(1L);
        order.verify(eventPublisher).publishEvent(EVENT);
        order.verify(ingestionService).process(2L);
        order.verify(ingestionService).process(3L);
        order.verify(retention).purge();
        order.verifyNoMoreInteractions();
    }

    @Test
    void processOffersSoloProcesaLosIdsIndicadosSinPaginarNiAplicarRetencion() {
        when(ingestionService.process(2L)).thenReturn(new Outcome(Result.NEW_PRICE, EVENT));
        when(ingestionService.process(3L)).thenReturn(new Outcome(Result.SKIPPED, null));

        StagingProcessor.RunSummary summary = processor.processOffers(List.of(2L, 3L));

        assertThat(summary.count(Result.NEW_PRICE)).isEqualTo(1);
        assertThat(summary.count(Result.SKIPPED)).isEqualTo(1);
        verify(eventPublisher).publishEvent(EVENT);
        verifyNoInteractions(offerRepository, retention);
    }

    @Test
    void unErrorDelListenerNoCambiaElResultadoNiDetieneElLote() {
        when(ingestionService.process(1L)).thenReturn(new Outcome(Result.NEW_PRICE, EVENT));
        when(ingestionService.process(2L)).thenReturn(new Outcome(Result.SAME_PRICE, null));
        when(ingestionService.process(3L)).thenReturn(new Outcome(Result.SAME_PRICE, null));
        doThrow(new IllegalStateException("aviso")).when(eventPublisher).publishEvent(EVENT);

        StagingProcessor.RunSummary summary = processor.runOnce();

        assertThat(summary.count(Result.NEW_PRICE)).isEqualTo(1);
        assertThat(summary.count(Result.SAME_PRICE)).isEqualTo(2);
        verify(retention).purge();
    }
}
