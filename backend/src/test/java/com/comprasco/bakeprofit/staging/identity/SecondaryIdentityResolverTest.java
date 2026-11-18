package com.comprasco.bakeprofit.staging.identity;

import static com.comprasco.bakeprofit.staging.StagingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.comprasco.bakeprofit.catalog.entity.Brand;
import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.staging.entity.StoreSkuEanMap;
import com.comprasco.bakeprofit.staging.repository.SecondaryCatalogRepository;
import com.comprasco.bakeprofit.staging.repository.StoreSkuEanMapRepository;

class SecondaryIdentityResolverTest {
    private static final double MINIMUM_SCORE = 0.90;
    private static final double MINIMUM_MARGIN = 0.08;
    private static final int CANDIDATE_LIMIT = 10;
    private static final String LOCAL_SKU = "8214";
    private final StoreSkuEanMapRepository mappings = mock(StoreSkuEanMapRepository.class);
    private final SecondaryCatalogRepository catalog = mock(SecondaryCatalogRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductNameSimilarity similarity = new NormalizedProductNameSimilarity();
    private final SecondaryIdentityResolver resolver = new SecondaryIdentityResolver(
            new SecondaryIdentityProperties(Set.of("bellapiel"), MINIMUM_SCORE, MINIMUM_MARGIN, CANDIDATE_LIMIT),
            mappings, catalog, products, similarity);

    private ScrapedOffer localOffer() {
        ScrapedOffer offer = offer("Cuidado facial");
        offer.getStore().setName("Bellapiel");
        when(offer.getEan()).thenReturn(LOCAL_SKU);
        return offer;
    }

    private Product candidate(ScrapedOffer offer, String name) {
        return Product.withIdentity(name, offer.getScrapeTarget().getCategory(), Brand.named("Diana"),
                GTIN_14, NET_CONTENT, UnitBase.G);
    }

    private void candidates(Product... candidates) {
        when(catalog.findCandidates(anyLong(), anyString(), any(), anyString(), any()))
                .thenReturn(List.of(candidates));
    }

    @Test
    void soloAutorizaCodigosCortosOAusentesDeTiendasConfiguradas() {
        ScrapedOffer offer = localOffer();
        assertThat(resolver.supports(offer)).isTrue();
        when(offer.getEan()).thenReturn(null);
        assertThat(resolver.supports(offer)).isTrue();
        when(offer.getEan()).thenReturn("7702552000142");
        assertThat(resolver.supports(offer)).isFalse();
        when(offer.getEan()).thenReturn("2000123456782");
        assertThat(resolver.supports(offer)).isFalse();
        when(offer.getEan()).thenReturn(LOCAL_SKU);
        offer.getStore().setName("Cutis");
        assertThat(resolver.supports(offer)).isFalse();
    }

    @Test
    void coincidenciaUnicaUsaProductoExistenteSinEscribirDuranteScreening() {
        ScrapedOffer offer = localOffer();
        Product product = candidate(offer, OFFER_NAME);
        candidates(product);
        assertThat(resolver.resolve(offer).product()).isSameAs(product);
        verify(mappings, never()).save(any());
        assertThat(offer.getEan()).isEqualTo(LOCAL_SKU);
        resolver.remember(offer, product);
        verify(mappings).save(argThat(mapping -> mapping.getStoreSku().equals(LOCAL_SKU)
                && mapping.getEan().equals(GTIN_14)));
    }

    @Test
    void equivalenciaGuardadaEvitaBuscarCandidatosYNoSeDuplica() {
        ScrapedOffer offer = localOffer();
        Product product = candidate(offer, OFFER_NAME);
        when(mappings.findByStoreIdAndStoreSku(1L, LOCAL_SKU))
                .thenReturn(Optional.of(StoreSkuEanMap.learned(1L, LOCAL_SKU, GTIN_14)));
        when(products.findByEan(GTIN_14)).thenReturn(Optional.of(product));
        assertThat(resolver.resolve(offer).product()).isSameAs(product);
        resolver.remember(offer, product);
        verifyNoInteractions(catalog);
        verify(mappings, never()).save(any());
        product.setNetContent(NET_CONTENT.multiply(java.math.BigDecimal.TEN));
        assertThat(resolver.resolve(offer).rejection()).contains("equivalencia guardada incompatible");
    }

    @Test
    void empateYFaltaDeMarcaNoCreanEquivalencia() {
        ScrapedOffer offer = localOffer();
        candidates(candidate(offer, OFFER_NAME), candidate(offer, OFFER_NAME));
        assertThat(resolver.resolve(offer).rejection()).contains("ambigua");
        when(offer.getBrandName()).thenReturn(null);
        assertThat(resolver.resolve(offer).rejection()).contains("falta marca");
        verify(mappings, never()).save(any());
    }

    @Test
    void marcaDistintaYTamanosDistintosNuncaSeCruzan() {
        ScrapedOffer offer = localOffer();
        Product product = candidate(offer, OFFER_NAME);
        product.setBrand(Brand.named("Otra marca"));
        candidates(product);
        assertThat(resolver.resolve(offer).rejection()).contains("sin identidad comparable o atributos compatibles");
        product.setBrand(Brand.named("Diana"));
        product.setUnitBase(UnitBase.ML);
        assertThat(resolver.resolve(offer).rejection()).contains("sin identidad comparable o atributos compatibles");
    }

    @Test
    void sinCatalogoNoCreaProductosNiMapeos() {
        ScrapedOffer offer = localOffer();
        assertThat(resolver.resolve(offer).rejection()).contains("sin candidatos por categoría, marca y presentación");
        verifyNoInteractions(products);
        verify(mappings, never()).save(any());
    }

    @Test
    void segundoCandidatoCercanoAunqueNoAlcanceUmbralImpideElegirGanador() {
        ScrapedOffer offer = localOffer();
        Product first = candidate(offer, "primer candidato");
        Product second = candidate(offer, "segundo candidato");
        ProductNameSimilarity metric = mock(ProductNameSimilarity.class);
        when(metric.score(OFFER_NAME, first.getName())).thenReturn(0.95);
        when(metric.score(OFFER_NAME, second.getName())).thenReturn(0.89);
        SecondaryIdentityResolver configured = new SecondaryIdentityResolver(
                new SecondaryIdentityProperties(Set.of("bellapiel"), MINIMUM_SCORE,
                        MINIMUM_MARGIN, CANDIDATE_LIMIT), mappings, catalog, products, metric);
        candidates(first, second);
        assertThat(configured.resolve(offer).rejection()).contains("ambigua");
    }

    @Test
    void demasiadosCandidatosNoPermitenElegirDeUnaMuestraTruncada() {
        ScrapedOffer offer = localOffer();
        Product product = candidate(offer, OFFER_NAME);
        when(catalog.findCandidates(anyLong(), anyString(), any(), anyString(), any()))
                .thenReturn(java.util.Collections.nCopies(CANDIDATE_LIMIT + 1, product));
        assertThat(resolver.resolve(offer).rejection()).contains("demasiados");
    }

    @Test
    void candidatosCompatiblesConNombreDistintoInformanPuntajeSinAprender() {
        ScrapedOffer offer = localOffer();
        candidates(candidate(offer, "Producto completamente diferente"));
        assertThat(resolver.resolve(offer).rejection())
                .contains("similitud de nombre insuficiente", "mejor=", "mínimo=0.9000");
        verify(mappings, never()).save(any());
    }

    @Test
    void equivalenciaIncompatibleIndicaElAtributoSinReemplazarElMapa() {
        ScrapedOffer offer = localOffer();
        Product product = candidate(offer, OFFER_NAME);
        when(mappings.findByStoreIdAndStoreSku(1L, LOCAL_SKU))
                .thenReturn(Optional.of(StoreSkuEanMap.learned(1L, LOCAL_SKU, GTIN_14)));
        when(products.findByEan(GTIN_14)).thenReturn(Optional.of(product));
        product.setUnitBase(UnitBase.ML);
        assertThat(resolver.resolve(offer).rejection()).contains("presentación o unidad distinta");
        product.setUnitBase(UnitBase.G);
        product.setBrand(Brand.named("Marca distinta"));
        assertThat(resolver.resolve(offer).rejection()).contains("marca distinta o ausente");
        verify(mappings, never()).save(any());
        verifyNoInteractions(catalog);
    }
}
