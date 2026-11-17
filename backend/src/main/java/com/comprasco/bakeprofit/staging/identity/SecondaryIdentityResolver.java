package com.comprasco.bakeprofit.staging.identity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.catalog.util.Gtin;
import com.comprasco.bakeprofit.catalog.util.GtinVerdict;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.staging.entity.StoreSkuEanMap;
import com.comprasco.bakeprofit.staging.repository.SecondaryCatalogRepository;
import com.comprasco.bakeprofit.staging.repository.StoreSkuEanMapRepository;

/** Resuelve códigos locales únicamente para tiendas autorizadas y productos ya existentes. */
@Component
public class SecondaryIdentityResolver {
    private static final Pattern LOCAL_CODE = Pattern.compile("[0-9]{1,7}");
    private static final int FIRST_CANDIDATE = 0;
    private static final int SECOND_CANDIDATE = 1;
    private static final int FIRST_PAGE = 0;
    private final SecondaryIdentityProperties properties;
    private final StoreSkuEanMapRepository mappings;
    private final SecondaryCatalogRepository catalog;
    private final ProductRepository products;
    private final ProductNameSimilarity similarity;

    public SecondaryIdentityResolver(SecondaryIdentityProperties properties,
            StoreSkuEanMapRepository mappings, SecondaryCatalogRepository catalog,
            ProductRepository products, ProductNameSimilarity similarity) {
        this.properties = properties;
        this.mappings = mappings;
        this.catalog = catalog;
        this.products = products;
        this.similarity = similarity;
    }

    public record Resolution(Product product, String rejection) {
        static Resolution rejected(String reason) {
            return new Resolution(null, reason);
        }
    }

    private record ScoredProduct(Product product, double score) {}

    public boolean supports(ScrapedOffer offer) {
        String rawEan = offer.getEan();
        boolean localIdentity = rawEan == null || rawEan.isBlank() || LOCAL_CODE.matcher(rawEan).matches();
        String storeName = NormalizedProductNameSimilarity.normalize(offer.getStore().getName());
        return localIdentity && properties.stores().stream()
                .map(NormalizedProductNameSimilarity::normalize).anyMatch(storeName::equals);
    }

    public Resolution resolve(ScrapedOffer offer) {
        if (!hasPresentation(offer)) {
            return Resolution.rejected("identidad secundaria: falta marca o presentación válida");
        }
        Optional<StoreSkuEanMap> mapping = localKey(offer)
                .flatMap(key -> mappings.findByStoreIdAndStoreSku(offer.getStore().getId(), key));
        if (mapping.isPresent()) {
            return resolveMapping(offer, mapping.get());
        }
        return matchAttributes(offer);
    }

    /** Se llama dentro de la ingesta transaccional, después del screening completo. */
    public void remember(ScrapedOffer offer, Product product) {
        if (!supports(offer)) {
            return;
        }
        localKey(offer).ifPresent(key -> {
            if (mappings.findByStoreIdAndStoreSku(offer.getStore().getId(), key).isEmpty()) {
                mappings.save(StoreSkuEanMap.learned(offer.getStore().getId(), key, product.getEan()));
            }
        });
    }

    private Resolution resolveMapping(ScrapedOffer offer, StoreSkuEanMap mapping) {
        Optional<Product> product = products.findByEan(mapping.getEan());
        if (product.isEmpty() || !compatible(offer, product.get())
                || similarity.score(offer.getName(), product.get().getName()) < properties.minimumScore()) {
            return Resolution.rejected("identidad secundaria: equivalencia guardada incompatible; requiere revisión");
        }
        return new Resolution(product.get(), null);
    }

    private Resolution matchAttributes(ScrapedOffer offer) {
        List<Product> candidates = catalog.findCandidates(offer.getScrapeTarget().getCategory().getId(),
                offer.getBrandName().trim(), offer.getNetContent(), offer.getUnitBase().trim().toUpperCase(java.util.Locale.ROOT),
                PageRequest.of(FIRST_PAGE, properties.candidateLimit() + 1));
        if (candidates.size() > properties.candidateLimit()) {
            return Resolution.rejected("identidad secundaria: demasiados candidatos; requiere revisión");
        }
        List<ScoredProduct> ranked = candidates.stream().filter(product -> compatible(offer, product))
                .map(product -> new ScoredProduct(product, similarity.score(offer.getName(), product.getName())))
                .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed()).toList();
        if (ranked.isEmpty() || ranked.get(FIRST_CANDIDATE).score() < properties.minimumScore()) {
            return Resolution.rejected("identidad secundaria: sin coincidencia confiable; requiere revisión");
        }
        if (ranked.size() > SECOND_CANDIDATE && ranked.get(FIRST_CANDIDATE).score()
                - ranked.get(SECOND_CANDIDATE).score() < properties.minimumMargin()) {
            return Resolution.rejected("identidad secundaria: coincidencia ambigua; requiere revisión");
        }
        return new Resolution(ranked.get(FIRST_CANDIDATE).product(), null);
    }

    private boolean compatible(ScrapedOffer offer, Product product) {
        return Boolean.TRUE.equals(product.getActive()) && product.getBrand() != null
                && Gtin.assess(product.getEan()) == GtinVerdict.COMPARABLE
                && product.getCategory().getId().equals(offer.getScrapeTarget().getCategory().getId())
                && product.hasSize(offer.getNetContent(), UnitBase.parse(offer.getUnitBase()).orElseThrow())
                && NormalizedProductNameSimilarity.normalize(product.getBrand().getName())
                    .equals(NormalizedProductNameSimilarity.normalize(offer.getBrandName()));
    }

    private static boolean hasPresentation(ScrapedOffer offer) {
        return offer.getBrandName() != null && !offer.getBrandName().isBlank()
                && offer.getNetContent() != null && offer.getNetContent().signum() > 0
                && UnitBase.parse(offer.getUnitBase()).isPresent();
    }

    private static Optional<String> localKey(ScrapedOffer offer) {
        String rawEan = offer.getEan();
        if (rawEan != null && LOCAL_CODE.matcher(rawEan).matches()) {
            return Optional.of(rawEan);
        }
        return Optional.ofNullable(offer.getStoreSku()).filter(key -> !key.isBlank());
    }
}
