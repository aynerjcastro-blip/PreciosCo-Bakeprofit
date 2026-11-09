package com.comprasco.bakeprofit.service;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import com.comprasco.bakeprofit.config.StagingProperties;
import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.entity.ScrapeTarget;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.entity.Store;
import com.comprasco.bakeprofit.entity.UnitBase;

/**
 * Datos compartidos por las pruebas del procesador. ScrapedOffer no tiene constructor
 * público (solo el scraper crea ofertas), así que se simula con un mock de getters.
 */
final class StagingFixtures {

    static final String SUPERMARKET = "Supermercado";
    static final String SPARE_PARTS = "Repuestos";
    static final String RESTRICTED_ONLY = "Ferretería";
    static final String EAN_13 = "7702552000141";
    static final String GTIN_14 = "07702552000141";
    static final BigDecimal PRICE = new BigDecimal("4500");
    static final BigDecimal NET_CONTENT = new BigDecimal("500");
    static final String OFFER_NAME = "Arroz Diana 500 g";
    static final String PRODUCT_URL = "https://tienda.co/arroz";
    static final String IMAGE_URL = "https://tienda.co/arroz.jpg";

    private StagingFixtures() {
    }

    /** Repuestos en ambas listas, como en producción; Ferretería solo exige marcas existentes. */
    static StagingProperties properties() {
        return new StagingProperties(false, 2, Duration.ofDays(2), Duration.ofDays(7),
                List.of(SPARE_PARTS, RESTRICTED_ONLY), List.of(SPARE_PARTS));
    }

    static StoreDomainPolicy domainPolicy() {
        return new StoreDomainPolicy(properties());
    }

    static Category root(String name) {
        Category category = new Category();
        category.setName(name);
        return category;
    }

    static Category leaf(Category parent) {
        Category category = new Category();
        category.setId(10L);
        category.setName("Granos y cereales");
        category.setParent(parent);
        return category;
    }

    static Store store(String domain) {
        Store store = new Store();
        store.setId(1L);
        store.setName("Tienda");
        store.setCategory(root(domain));
        return store;
    }

    static ScrapeTarget target(Store store) {
        return ScrapeTarget.of(store, leaf(store.getCategory()), "arroz", UnitBase.G,
                new BigDecimal("400"), new BigDecimal("1000"));
    }

    /** Oferta válida de supermercado; cada prueba cambia solo lo que le interesa. */
    static ScrapedOffer offer(String domain) {
        Store store = store(domain);
        ScrapedOffer offer = mock(ScrapedOffer.class);
        lenient().when(offer.getId()).thenReturn(100L);
        lenient().when(offer.getStore()).thenReturn(store);
        lenient().when(offer.getScrapeTarget()).thenReturn(target(store));
        lenient().when(offer.getEan()).thenReturn(EAN_13);
        lenient().when(offer.getName()).thenReturn(OFFER_NAME);
        lenient().when(offer.getBrandName()).thenReturn("Diana");
        lenient().when(offer.getPrice()).thenReturn(PRICE);
        lenient().when(offer.getNetContent()).thenReturn(NET_CONTENT);
        lenient().when(offer.getUnitBase()).thenReturn("G");
        lenient().when(offer.getProductUrl()).thenReturn(PRODUCT_URL);
        lenient().when(offer.getImageUrl()).thenReturn(IMAGE_URL);
        lenient().when(offer.isPending()).thenReturn(true);
        return offer;
    }
}
