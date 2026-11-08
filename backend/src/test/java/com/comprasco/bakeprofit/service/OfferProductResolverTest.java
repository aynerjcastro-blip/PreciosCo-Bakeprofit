package com.comprasco.bakeprofit.service;

import static com.comprasco.bakeprofit.service.StagingFixtures.GTIN_14;
import static com.comprasco.bakeprofit.service.StagingFixtures.NET_CONTENT;
import static com.comprasco.bakeprofit.service.StagingFixtures.RESTRICTED_ONLY;
import static com.comprasco.bakeprofit.service.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.service.StagingFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.entity.Brand;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.entity.UnitBase;
import com.comprasco.bakeprofit.repository.BrandRepository;
import com.comprasco.bakeprofit.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OfferProductResolverTest {

    private static final OfferScreening.Accepted ACCEPTED = new OfferScreening.Accepted(GTIN_14, UnitBase.G);

    @Mock
    private ProductRepository productRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private BrandService brandService;

    private OfferProductResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OfferProductResolver(productRepository, brandRepository, brandService,
                StagingFixtures.domainPolicy());
    }

    private void productIsNew() {
        when(productRepository.findByEan(GTIN_14)).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void productoNuevoSeCreaConSuIdentidadYLaCategoriaDelObjetivo() {
        ScrapedOffer offer = offer(SUPERMARKET);
        Brand diana = Brand.named("Diana");
        productIsNew();
        when(brandService.resolveByName("Diana")).thenReturn(diana);

        Product product = resolver.resolve(offer, ACCEPTED).product();

        assertThat(product.getName()).isEqualTo(StagingFixtures.OFFER_NAME);
        assertThat(product.getEan()).isEqualTo(GTIN_14);
        assertThat(product.getNetContent()).isEqualByComparingTo(NET_CONTENT);
        assertThat(product.getUnitBase()).isEqualTo(UnitBase.G);
        assertThat(product.getUnit()).isEqualTo("500 g");
        assertThat(product.getBrand()).isSameAs(diana);
        assertThat(product.getCategory()).isSameAs(offer.getScrapeTarget().getCategory());
    }

    @Test
    void productoNuevoSinMarcaSeCreaSinMarca() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getBrandName()).thenReturn("  ");
        productIsNew();

        Product product = resolver.resolve(offer, ACCEPTED).product();

        assertThat(product.getBrand()).isNull();
        verifyNoInteractions(brandService, brandRepository);
    }

    @Test
    void productoExistenteSeUsaSinModificarloNiResolverMarcas() {
        Product existing = Product.withIdentity("Arroz Diana", null, null, GTIN_14,
                new BigDecimal("500.00"), UnitBase.G);
        when(productRepository.findByEan(GTIN_14)).thenReturn(Optional.of(existing));

        OfferProductResolver.Match match = resolver.resolve(offer(SUPERMARKET), ACCEPTED);

        assertThat(match.product()).isSameAs(existing);
        assertThat(existing.getName()).isEqualTo("Arroz Diana");
        verify(productRepository, never()).save(any());
        verifyNoInteractions(brandService, brandRepository);
    }

    @Test
    void productoExistenteConOtroTamanoQuedaSinClasificar() {
        Product existing = Product.withIdentity("Arroz Diana 1 kg", null, null, GTIN_14,
                new BigDecimal("1000"), UnitBase.G);
        when(productRepository.findByEan(GTIN_14)).thenReturn(Optional.of(existing));

        OfferProductResolver.Match match = resolver.resolve(offer(SUPERMARKET), ACCEPTED);

        assertThat(match.rejection()).isEqualTo(OfferProductResolver.DIFFERENT_SIZE_REASON
                .formatted("Arroz Diana 1 kg", "1 kg", "500 g"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void marcaDesconocidaEnUnDominioRestringidoQuedaSinClasificarYNoSeCrea() {
        when(productRepository.findByEan(GTIN_14)).thenReturn(Optional.empty());
        when(brandRepository.findByNameMatchingIndex("Diana")).thenReturn(Optional.empty());

        OfferProductResolver.Match match = resolver.resolve(offer(RESTRICTED_ONLY), ACCEPTED);

        assertThat(match.rejection()).isEqualTo(OfferProductResolver.UNKNOWN_BRAND_REASON.formatted("Diana"));
        verifyNoInteractions(brandService);
        verify(productRepository, never()).save(any());
    }

    @Test
    void marcaExistenteEnUnDominioRestringidoSeEmpareja() {
        Brand diana = Brand.named("Diana");
        productIsNew();
        when(brandRepository.findByNameMatchingIndex("Diana")).thenReturn(Optional.of(diana));

        Product product = resolver.resolve(offer(RESTRICTED_ONLY), ACCEPTED).product();

        assertThat(product.getBrand()).isSameAs(diana);
        verifyNoInteractions(brandService);
    }
}
