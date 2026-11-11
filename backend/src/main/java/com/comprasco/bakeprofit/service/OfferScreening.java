package com.comprasco.bakeprofit.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.entity.ScrapeTarget;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.entity.UnitBase;
import com.comprasco.bakeprofit.util.Gtin;

/**
 * Filtros de una oferta que solo miran sus propios datos: no consultan ni escriben la BD.
 * Se evalúan en orden y gana el primer motivo de rechazo, que queda en {@code reason}.
 *
 * <p>
 * Corren antes de cualquier escritura: una oferta rechazada no deja productos, marcas
 * ni precios a medias.
 * </p>
 */
@Component
public class OfferScreening {

    static final String NO_TARGET_REASON = "sin objetivo de búsqueda";
    static final String NO_EAN_REASON = "La oferta no trae EAN";
    static final String NO_CONTENT_REASON = "La oferta no trae el contenido neto";
    static final String UNKNOWN_UNIT_REASON = "Unidad base desconocida: '%s'";
    static final String OUT_OF_TARGET_REASON =
            "El tamaño %s %s no cumple el objetivo de búsqueda '%s' (%s, de %s a %s)";
    static final String INVALID_CATEGORY_REASON =
            "La categoría '%s' del objetivo de búsqueda no es una categoría hoja activa";
    static final String PENDING_IDENTITY_REASON = "identidad por referencia pendiente (Sprint 3)";
    private static final String OPEN_LIMIT = "sin límite";

    private final StoreDomainPolicy domainPolicy;

    public OfferScreening(StoreDomainPolicy domainPolicy) {
        this.domainPolicy = domainPolicy;
    }

    /** Oferta que pasó los filtros, con el EAN ya normalizado y la unidad ya interpretada. */
    public record Accepted(String gtin, UnitBase unitBase) {
    }

    /** Exactamente uno de los dos campos tiene valor. */
    public record Result(Accepted accepted, String rejection) {

        static Result rejected(String reason) {
            return new Result(null, reason);
        }

        public boolean isRejected() {
            return rejection != null;
        }
    }

    public Result screen(ScrapedOffer offer) {
        return Stream.<Supplier<Optional<String>>>of(
                    () -> missingTarget(offer),
                    () -> gtinRejection(offer.getEan()),
                    () -> missingContent(offer),
                    () -> unknownUnit(offer.getUnitBase()),
                    () -> outsideTarget(offer),
                    () -> invalidCategory(offer.getScrapeTarget().getCategory()),
                    () -> pendingIdentity(offer))
                .map(Supplier::get)
                .flatMap(Optional::stream)
                .findFirst()
                .map(Result::rejected)
                .orElseGet(() -> new Result(accepted(offer), null));
    }

    private static Optional<String> missingTarget(ScrapedOffer offer) {
        return offer.getScrapeTarget() == null ? Optional.of(NO_TARGET_REASON) : Optional.empty();
    }

    private static Optional<String> gtinRejection(String ean) {
        if (ean == null || ean.isBlank()) {
            return Optional.of(NO_EAN_REASON);
        }
        return Gtin.assess(ean).rejectionMessage(ean);
    }

    private static Optional<String> missingContent(ScrapedOffer offer) {
        return offer.getNetContent() == null ? Optional.of(NO_CONTENT_REASON) : Optional.empty();
    }

    private static Optional<String> unknownUnit(String rawUnit) {
        return UnitBase.parse(rawUnit).isPresent()
                ? Optional.empty()
                : Optional.of(UNKNOWN_UNIT_REASON.formatted(rawUnit));
    }

    private static Optional<String> outsideTarget(ScrapedOffer offer) {
        ScrapeTarget target = offer.getScrapeTarget();
        UnitBase unit = UnitBase.parse(offer.getUnitBase()).orElseThrow();
        if (target.accepts(offer.getNetContent(), unit)) {
            return Optional.empty();
        }
        return Optional.of(OUT_OF_TARGET_REASON.formatted(
                offer.getNetContent().toPlainString(), unit, target.getSearchTerm(),
                target.getUnitBase(), limit(target.getMinContent()), limit(target.getMaxContent())));
    }

    private static Optional<String> invalidCategory(Category category) {
        boolean leaf = category.getParent() != null;
        boolean active = Boolean.TRUE.equals(category.getActive());
        return leaf && active ? Optional.empty() : Optional.of(INVALID_CATEGORY_REASON.formatted(category.getName()));
    }

    private Optional<String> pendingIdentity(ScrapedOffer offer) {
        return domainPolicy.hasPendingIdentity(offer.getStore())
                ? Optional.of(PENDING_IDENTITY_REASON)
                : Optional.empty();
    }

    private static Accepted accepted(ScrapedOffer offer) {
        return new Accepted(Gtin.normalize(offer.getEan()), UnitBase.parse(offer.getUnitBase()).orElseThrow());
    }

    private static String limit(BigDecimal value) {
        return value == null ? OPEN_LIMIT : value.toPlainString();
    }
}
