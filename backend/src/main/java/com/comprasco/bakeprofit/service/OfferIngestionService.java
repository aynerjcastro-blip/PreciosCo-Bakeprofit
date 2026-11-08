package com.comprasco.bakeprofit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.entity.Price;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.repository.ScrapedOfferRepository;

/**
 * Convierte UNA oferta del staging en producto y precio, en su propia transacción.
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
        /** Ya no estaba PENDING (otra instancia la procesó o se borró). */
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
        return offerRepository.lockById(offerId)
                .filter(ScrapedOffer::isPending)
                .map(this::ingest)
                .orElseGet(() -> Outcome.of(Result.SKIPPED));
    }

    private Outcome ingest(ScrapedOffer offer) {
        OfferScreening.Result screened = screening.screen(offer);
        if (screened.isRejected()) {
            return unclassify(offer, screened.rejection());
        }
        OfferProductResolver.Match match = productResolver.resolve(offer, screened.accepted());
        if (match.isRejected()) {
            return unclassify(offer, match.rejection());
        }
        Outcome outcome = priceRecorder.record(match.product(), offer)
                .map(Outcome::newPrice)
                .orElseGet(() -> Outcome.of(Result.SAME_PRICE));
        offer.markProcessed();
        return outcome;
    }

    private static Outcome unclassify(ScrapedOffer offer, String reason) {
        offer.markUnclassified(reason);
        return Outcome.of(Result.UNCLASSIFIED);
    }
}
