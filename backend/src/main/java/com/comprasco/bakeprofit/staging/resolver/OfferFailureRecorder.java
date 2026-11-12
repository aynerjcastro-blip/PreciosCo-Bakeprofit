package com.comprasco.bakeprofit.staging.resolver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;

/**
 * Deja sin clasificar una oferta cuyo procesamiento lanzó una excepción.
 *
 * <p>
 * Corre en una transacción nueva y separada, nunca anidada: la del procesamiento ya se
 * deshizo y devolvió su conexión, y en PostgreSQL no se puede seguir escribiendo en una
 * transacción donde falló una sentencia.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class OfferFailureRecorder {

    static final String FAILURE_REASON = "Error al procesar la oferta: %s: %s";

    private final ScrapedOfferRepository offerRepository;

    public OfferFailureRecorder(ScrapedOfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Transactional
    public void markFailed(Long offerId, RuntimeException cause) {
        String reason = FAILURE_REASON.formatted(cause.getClass().getSimpleName(), cause.getMessage());
        offerRepository.lockById(offerId)
                .filter(ScrapedOffer::isPending)
                .ifPresent(offer -> offer.markUnclassified(reason));
    }
}
