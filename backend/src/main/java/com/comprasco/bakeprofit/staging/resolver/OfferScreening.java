package com.comprasco.bakeprofit.staging.resolver;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.util.GtinVerdict;
import com.comprasco.bakeprofit.staging.entity.StoreProductLink;
import com.comprasco.bakeprofit.staging.repository.EanSizeOverrideRepository;
import com.comprasco.bakeprofit.staging.repository.StoreProductLinkRepository;
import com.comprasco.bakeprofit.staging.identity.SecondaryIdentityResolver;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.util.Gtin;
import com.comprasco.bakeprofit.staging.entity.ScrapeTarget;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.store.rule.StoreDomainPolicy;

/**
 * Valida identidad, tamaño efectivo y curaduría manual sin modificar la oferta cruda.
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
    private final SecondaryIdentityResolver secondaryIdentity;

    private final EanSizeOverrideRepository overrides;
    private final StoreProductLinkRepository links;

    public OfferScreening(StoreDomainPolicy domainPolicy,
            EanSizeOverrideRepository overrides,
            StoreProductLinkRepository links, SecondaryIdentityResolver secondaryIdentity) {
        this.secondaryIdentity = secondaryIdentity;
        this.domainPolicy = domainPolicy;
        this.overrides = overrides;
        this.links = links;
    }

    /** Oferta que pasó los filtros, con el EAN ya normalizado y la unidad ya interpretada. */
    public record Accepted(String gtin, UnitBase unitBase, BigDecimal netContent,
                           Product linkedProduct, BigDecimal price) {
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
        return screen(offer, true);
    }

    /**
     * Evidencia para comparar GTIN: omite el rango del target, no autoriza la ingesta.
     * Las rutas de identidad secundaria y peso variable conservan sus propios filtros.
     */
    public Result screenSizeEvidence(ScrapedOffer offer) {
        return screen(offer, false);
    }

    private Result screen(ScrapedOffer offer, boolean requireTargetMatch) {
        if (offer.getScrapeTarget() == null) return Result.rejected(NO_TARGET_REASON);
        ScrapeTarget target = offer.getScrapeTarget();
        if (!Boolean.TRUE.equals(target.getActive())) return Result.rejected("objetivo de búsqueda inactivo");
        if (!target.getStore().getId().equals(offer.getStore().getId()))
            return Result.rejected("la tienda de la oferta no coincide con el objetivo");
        if (offer.getPrice() == null || offer.getPrice().signum() <= 0)
            return Result.rejected("precio no positivo");
        Optional<String> categoryError = invalidCategory(target.getCategory());
        if (categoryError.isPresent()) return Result.rejected(categoryError.get());
        Optional<String> identityError = pendingIdentity(offer);
        if (identityError.isPresent()) return Result.rejected(identityError.get());
        if (secondaryIdentity.supports(offer)) {
            return screenSecondaryIdentity(offer);
        }
        String expected = target.getExpectedEan();
        if (expected != null && (Gtin.assess(expected) != GtinVerdict.COMPARABLE
                || Gtin.assess(offer.getEan()) != GtinVerdict.COMPARABLE
                || !Gtin.normalize(expected).equals(Gtin.normalize(offer.getEan()))))
            return Result.rejected("el EAN " + offer.getEan() + " no es el que espera el objetivo, " + expected);

        var verdict = Gtin.assess(offer.getEan());
        if (verdict == GtinVerdict.STORE_INTERNAL
                || verdict == GtinVerdict.VARIABLE_MEASURE) {
            return variableWeight(offer);
        }
        Optional<String> eanError = gtinRejection(offer.getEan());
        if (eanError.isPresent()) return Result.rejected(eanError.get());
        String gtin = Gtin.normalize(offer.getEan());
        BigDecimal content = offer.getNetContent();
        UnitBase unit = UnitBase.parse(offer.getUnitBase()).orElse(null);
        var override = overrides.findById(gtin);
        if (override.isPresent()) {
            content = override.get().getNetContent();
            unit = override.get().getUnitBase();
        } else if ((content == null || offer.getUnitBase() == null) && expected != null
                && target.getMinContent() != null && target.getMaxContent() != null
                && target.getMinContent().compareTo(target.getMaxContent()) == 0
                && (content == null || content.compareTo(target.getMinContent()) == 0)
                && (offer.getUnitBase() == null || unit == target.getUnitBase())) {
            content = target.getMinContent();
            unit = target.getUnitBase();
        }
        if (content == null) return Result.rejected(NO_CONTENT_REASON);
        if (unit == null) return Result.rejected(UNKNOWN_UNIT_REASON.formatted(offer.getUnitBase()));
        if (content.signum() <= 0 || (requireTargetMatch && !target.accepts(content, unit)))
            return Result.rejected(OUT_OF_TARGET_REASON.formatted(content.toPlainString(), unit,
                    target.getSearchTerm(), target.getUnitBase(), limit(target.getMinContent()), limit(target.getMaxContent())));
        return new Result(new Accepted(gtin, unit, content, null, offer.getPrice()), null);
    }

    private Result screenSecondaryIdentity(ScrapedOffer offer) {
        SecondaryIdentityResolver.Resolution resolution = secondaryIdentity.resolve(offer);
        if (resolution.rejection() != null) {
            return Result.rejected(resolution.rejection());
        }
        Product product = resolution.product();
        ScrapeTarget target = offer.getScrapeTarget();
        String expectedEan = target.getExpectedEan();
        if (expectedEan != null && (Gtin.assess(expectedEan) != GtinVerdict.COMPARABLE
                || !Gtin.normalize(expectedEan).equals(product.getEan()))) {
            return Result.rejected("identidad secundaria: no coincide con el EAN esperado del objetivo");
        }
        if (!target.accepts(product.getNetContent(), product.getUnitBase())) {
            return Result.rejected("identidad secundaria: presentación fuera del objetivo");
        }
        return new Result(new Accepted(product.getEan(), product.getUnitBase(),
                product.getNetContent(), product, offer.getPrice()), null);
    }

    private Result variableWeight(ScrapedOffer offer) {
        var link = offer.getStoreSku() == null ? Optional.<StoreProductLink>empty()
                : links.findByStoreIdAndStoreSku(offer.getStore().getId(), offer.getStoreSku());
        if (link.isEmpty()) return Result.rejected("peso variable sin asignar");
        var product = link.get().getProduct();
        if (!Boolean.TRUE.equals(product.getActive()) || product.getBrand() != null
                || product.getEan() != null || product.getManufacturerRef() != null
                || !product.hasSize(new BigDecimal("1000"), UnitBase.G)
                || invalidCategory(product.getCategory()).isPresent()
                || !product.getCategory().getId().equals(offer.getScrapeTarget().getCategory().getId()))
            return Result.rejected("asignación de peso variable inválida: se exige un genérico de 1000 G en la categoría del objetivo");
        if (offer.getSaleWeightKg() == null || offer.getSaleWeightKg().signum() <= 0)
            return Result.rejected("peso variable sin peso de venta publicado en kg");
        BigDecimal perKg = offer.getPrice().divide(offer.getSaleWeightKg(), 2, RoundingMode.HALF_UP);
        if (perKg.signum() <= 0 || perKg.precision() > 12) return Result.rejected("precio por kg fuera de rango");
        return new Result(new Accepted(null, UnitBase.G, new BigDecimal("1000"), product, perKg), null);
    }

    private static Optional<String> gtinRejection(String ean) {
        if (ean == null || ean.isBlank()) {
            return Optional.of(NO_EAN_REASON);
        }
        return Gtin.assess(ean).rejectionMessage(ean);
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

    private static String limit(BigDecimal value) {
        return value == null ? OPEN_LIMIT : value.toPlainString();
    }
}
