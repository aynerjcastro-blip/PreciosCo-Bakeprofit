package com.comprasco.bakeprofit.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.entity.Brand;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.repository.BrandRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;
import com.comprasco.bakeprofit.util.NetContentLabel;

/**
 * Empareja una oferta ya filtrada con un producto del catálogo por su EAN normalizado,
 * o lo crea si el EAN es nuevo. Corre dentro de la transacción de la oferta.
 *
 * <p>
 * Un producto existente no se modifica: si la oferta trae otro tamaño, el EAN está mal
 * usado en alguna parte y unirlos mezclaría presentaciones distintas (RN-02). La marca
 * solo se resuelve al crear: buscarla o crearla para un producto que ya existe dejaría
 * marcas que nadie usa.
 * </p>
 */
@Component
public class OfferProductResolver {

    static final String DIFFERENT_SIZE_REASON =
            "El producto '%s' ya existe con otro tamaño (%s); la oferta trae %s";
    static final String UNKNOWN_BRAND_REASON =
            "Marca desconocida '%s': en este dominio solo se emparejan marcas existentes";
    private static final String NO_SIZE = "sin tamaño";

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final BrandService brandService;
    private final StoreDomainPolicy domainPolicy;

    public OfferProductResolver(ProductRepository productRepository, BrandRepository brandRepository,
                                BrandService brandService, StoreDomainPolicy domainPolicy) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.brandService = brandService;
        this.domainPolicy = domainPolicy;
    }

    /** Exactamente uno de los dos campos tiene valor. */
    public record Match(Product product, String rejection) {

        static Match of(Product product) {
            return new Match(product, null);
        }

        static Match rejected(String reason) {
            return new Match(null, reason);
        }

        public boolean isRejected() {
            return rejection != null;
        }
    }

    public Match resolve(ScrapedOffer offer, OfferScreening.Accepted accepted) {
        return productRepository.findByEan(accepted.gtin())
                .map(existing -> matchExisting(existing, offer, accepted))
                .orElseGet(() -> createProduct(offer, accepted));
    }

    private static Match matchExisting(Product existing, ScrapedOffer offer, OfferScreening.Accepted accepted) {
        if (existing.hasSize(offer.getNetContent(), accepted.unitBase())) {
            return Match.of(existing);
        }
        return Match.rejected(DIFFERENT_SIZE_REASON.formatted(
                existing.getName(), sizeOf(existing),
                NetContentLabel.format(offer.getNetContent(), accepted.unitBase())));
    }

    private Match createProduct(ScrapedOffer offer, OfferScreening.Accepted accepted) {
        String brandName = offer.getBrandName();
        if (!hasText(brandName)) {
            return Match.of(saveProduct(offer, accepted, null));
        }
        if (!domainPolicy.requiresExistingBrand(offer.getStore())) {
            return Match.of(saveProduct(offer, accepted, brandService.resolveByName(brandName)));
        }
        Optional<Brand> existingBrand = brandRepository.findByNameMatchingIndex(brandName.trim());
        return existingBrand
                .map(brand -> Match.of(saveProduct(offer, accepted, brand)))
                .orElseGet(() -> Match.rejected(UNKNOWN_BRAND_REASON.formatted(brandName.trim())));
    }

    private Product saveProduct(ScrapedOffer offer, OfferScreening.Accepted accepted, Brand brand) {
        return productRepository.save(Product.withIdentity(
                offer.getName(), offer.getScrapeTarget().getCategory(), brand,
                accepted.gtin(), offer.getNetContent(), accepted.unitBase()));
    }

    private static String sizeOf(Product product) {
        return product.hasNetContent()
                ? NetContentLabel.format(product.getNetContent(), product.getUnitBase())
                : NO_SIZE;
    }

    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
