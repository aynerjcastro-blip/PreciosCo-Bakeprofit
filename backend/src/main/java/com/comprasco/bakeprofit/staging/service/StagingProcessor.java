package com.comprasco.bakeprofit.staging.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.notification.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.staging.repository.PendingOfferKey;
import com.comprasco.bakeprofit.staging.config.StagingProperties;
import com.comprasco.bakeprofit.staging.entity.OfferStatus;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;
import com.comprasco.bakeprofit.staging.resolver.OfferFailureRecorder;
import com.comprasco.bakeprofit.staging.service.OfferIngestionService.Outcome;
import com.comprasco.bakeprofit.staging.service.OfferIngestionService.Result;

/**
 * Orquesta una corrida del procesador de staging: recorre las ofertas PENDING, procesa
 * cada una en su propia transacción, publica los precios nuevos y aplica la retención.
 *
 * <p>
 * No es transaccional a propósito: cada paso toma una conexión y la devuelve antes del
 * siguiente, así que nunca hay dos a la vez (pool de 2, RP-04). Un error en una oferta
 * no detiene el lote. La tarea programada vive en {@link StagingScheduler}; este bean
 * existe siempre para poder ejecutarlo desde las pruebas.
 * </p>
 */
@Component
public class StagingProcessor {

    private static final Logger log = LoggerFactory.getLogger(StagingProcessor.class);

    private final ScrapedOfferRepository offerRepository;
    private final OfferIngestionService ingestionService;
    private final OfferFailureRecorder failureRecorder;
    private final StagingRetention retention;
    private final ApplicationEventPublisher eventPublisher;
    private final Pageable page;

    public StagingProcessor(ScrapedOfferRepository offerRepository, OfferIngestionService ingestionService,
                            OfferFailureRecorder failureRecorder, StagingRetention retention,
                            ApplicationEventPublisher eventPublisher, StagingProperties properties) {
        this.offerRepository = offerRepository;
        this.ingestionService = ingestionService;
        this.failureRecorder = failureRecorder;
        this.retention = retention;
        this.eventPublisher = eventPublisher;
        this.page = PageRequest.ofSize(properties.batchSize());
    }

    /** Cantidad de ofertas por resultado en una corrida. */
    public static final class RunSummary {

        private final Map<Result, Integer> counts = new EnumMap<>(Result.class);

        void add(Result result) {
            counts.merge(result, 1, Integer::sum);
        }

        public int count(Result result) {
            return counts.getOrDefault(result, 0);
        }

        @Override
        public String toString() {
            return counts.toString();
        }
    }

    /** Una corrida completa: todas las ofertas PENDING y, al final, la retención. */
    public RunSummary runOnce() {
        RunSummary summary = new RunSummary();
        List<PendingOfferKey> keys = offerRepository.findKeysByStatus(OfferStatus.PENDING, page);
        while (!keys.isEmpty()) {
            processInto(summary, keys.stream().map(PendingOfferKey::id).toList());
            PendingOfferKey last = keys.getLast();
            keys = offerRepository.findKeysByStatusAfter(
                    OfferStatus.PENDING, last.scrapedAt(), last.id(), page);
        }
        log.info("Procesador de staging: {}", summary);
        purgeOldOffers();
        return summary;
    }

    /**
     * Procesa solo las ofertas indicadas, con las mismas garantías que una corrida (una
     * transacción por oferta, el fallo marcado aparte, el evento después del commit),
     * pero sin paginar ni aplicar la retención. Una oferta que ya no está PENDING se omite.
     *
     * <p>
     * Permite procesar un conjunto acotado sin tocar el resto del staging: lo usa la
     * prueba de integración y sirve para reprocesar ofertas puntuales.
     * </p>
     */
    public RunSummary processOffers(List<Long> offerIds) {
        RunSummary summary = new RunSummary();
        processInto(summary, offerIds);
        return summary;
    }

    private void processInto(RunSummary summary, List<Long> offerIds) {
        offerIds.forEach(offerId -> summary.add(processSafely(offerId)));
    }

    private Result processSafely(Long offerId) {
        Outcome outcome;
        try {
            outcome = ingestionService.process(offerId);
        } catch (RuntimeException e) {
            log.error("Procesador de staging: falló la oferta {}", offerId, e);
            markFailed(offerId, e);
            return Result.FAILED;
        }
        if (outcome.hasEvent()) {
            publish(outcome.event());
        }
        return outcome.result();
    }

    private void markFailed(Long offerId, RuntimeException cause) {
        try {
            failureRecorder.markFailed(offerId, cause);
        } catch (RuntimeException e) {
            log.error("Procesador de staging: no se pudo marcar la oferta {} como UNCLASSIFIED", offerId, e);
        }
    }

    // Después del commit y fuera de toda transacción (ver PriceRegisteredEvent)
    private void publish(PriceRegisteredEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (RuntimeException e) {
            log.error("Procesador de staging: falló el aviso del precio {}", event.priceId(), e);
        }
    }

    private void purgeOldOffers() {
        try {
            retention.purge();
        } catch (RuntimeException e) {
            log.error("Procesador de staging: falló la retención de scraped_offers", e);
        }
    }
}
