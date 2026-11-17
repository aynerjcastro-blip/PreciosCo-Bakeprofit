package com.comprasco.bakeprofit.staging.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.notification.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.price.entity.Price;
import com.comprasco.bakeprofit.price.service.PriceRecorder;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;
import com.comprasco.bakeprofit.staging.resolver.OfferProductResolver;
import com.comprasco.bakeprofit.staging.resolver.OfferScreening;

/**
 * Resuelve una oferta y sus duplicadas de la corrida en una sola transacción.
 *
 * <p>
 * Es un bean distinto del orquestador para que el proxy de Spring aplique
 * {@code @Transactional}: cada llamada toma una conexión, hace commit y la devuelve
 * antes de la siguiente oferta. Los rechazos esperados se marcan aquí mismo, porque
 * ninguna sentencia falló; un error inesperado sale como excepción, la transacción se
 * deshace entera y el orquestador marca la oferta en otra transacción.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class OfferIngestionService {

    private final ScrapedOfferRepository offerRepository;
    private final OfferScreening screening;
    private final OfferProductResolver productResolver;
    private final PriceRecorder priceRecorder;

    public OfferIngestionService(ScrapedOfferRepository offerRepository, OfferScreening screening,
                                 OfferProductResolver productResolver, PriceRecorder priceRecorder) {
        this.offerRepository = offerRepository;
        this.screening = screening;
        this.productResolver = productResolver;
        this.priceRecorder = priceRecorder;
    }

    public enum Result {
        /** Precio distinto al anterior: fila nueva en prices y evento. */
        NEW_PRICE,
        /** Mismo precio: solo se refrescó el periodo vigente. */
        SAME_PRICE,
        UNCLASSIFIED,
        /**
         * Ya no estaba PENDING: otra instancia la procesó, se borró o la marcó el conflicto de
         * tamaño de otra oferta del mismo EAN ({@link SizeConflictCheck}).
         */
        SKIPPED,
        /** Error inesperado; solo lo asigna el orquestador. */
        FAILED
    }

    /** {@code event} solo tiene valor cuando {@code result} es NEW_PRICE. */
    public record Outcome(Result result, PriceRegisteredEvent event) {

        static Outcome of(Result result) {
            return new Outcome(result, null);
        }

        static Outcome newPrice(Price price) {
            return new Outcome(Result.NEW_PRICE, new PriceRegisteredEvent(
                    price.getId(), price.getProduct().getId(), price.getStore().getId()));
        }

        public boolean hasEvent() {
            return event != null;
        }
    }

    @Transactional
    public Outcome process(Long offerId) {
        offerRepository.lockIngestion();
        return offerRepository.lockById(offerId)
                .filter(ScrapedOffer::isPending)
                .filter(o -> o.getRunId() == null || offerRepository.isRunComplete(o.getRunId()))
                .map(this::ingest)
                .orElseGet(() -> Outcome.of(Result.SKIPPED));
    }

    private Outcome ingest(ScrapedOffer offer) {
        OfferScreening.Result screened = screening.screen(offer);
        if (screened.isRejected()) {
            return unclassify(offer, screened.rejection());
        }
        List<Candidate> duplicates = new ArrayList<>();
        if (offer.getRunId() != null) {
            for (ScrapedOffer other : offerRepository.lockRunOffers(offer.getRunId(), offer.getStore().getId(),
                    screened.accepted().gtin(), screened.accepted().linkedProduct() == null
                            ? null : screened.accepted().linkedProduct().getId())) {
                OfferScreening.Result result = screening.screen(other);
                if (!result.isRejected() && sameIdentity(screened.accepted(), result.accepted()))
                    duplicates.add(new Candidate(other, result.accepted()));
            }
        }
        if (!duplicates.isEmpty()) {
            Candidate winner = duplicates.stream().min(Comparator
                    .comparing((Candidate c) -> c.accepted().price())
                    .thenComparing(c -> c.offer().getId())).orElseThrow();
            offer = winner.offer();
            screened = new OfferScreening.Result(winner.accepted(), null);
        }
        OfferProductResolver.Match match = productResolver.resolve(offer, screened.accepted());
        if (match.isRejected()) {
            return unclassify(offer, match.rejection());
        }
        Outcome outcome = priceRecorder.record(match.product(), offer, screened.accepted().price())
                .map(Outcome::newPrice)
                .orElseGet(() -> Outcome.of(Result.SAME_PRICE));
        offer.markProcessed();
        for (Candidate candidate : duplicates) {
            if (!candidate.offer().getId().equals(offer.getId())) candidate.offer().markDuplicate();
        }
        return outcome;
    }

    private record Candidate(ScrapedOffer offer, OfferScreening.Accepted accepted) {}

    private static boolean sameIdentity(OfferScreening.Accepted a, OfferScreening.Accepted b) {
        if (a.linkedProduct() != null || b.linkedProduct() != null)
            return a.linkedProduct() != null && b.linkedProduct() != null
                    && a.linkedProduct().getId().equals(b.linkedProduct().getId());
        return a.gtin().equals(b.gtin());
    }

    private static Outcome unclassify(ScrapedOffer offer, String reason) {
        offer.markUnclassified(reason);
        return Outcome.of(Result.UNCLASSIFIED);
    }
}
