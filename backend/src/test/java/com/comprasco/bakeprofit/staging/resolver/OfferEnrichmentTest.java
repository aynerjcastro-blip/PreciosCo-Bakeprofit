package com.comprasco.bakeprofit.staging.resolver;
import static com.comprasco.bakeprofit.staging.StagingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.comprasco.bakeprofit.catalog.entity.*;
import com.comprasco.bakeprofit.staging.entity.*;
import com.comprasco.bakeprofit.staging.repository.*;

class OfferEnrichmentTest {
    private final EanSizeOverrideRepository overrides = mock(EanSizeOverrideRepository.class);
    private final StoreProductLinkRepository links = mock(StoreProductLinkRepository.class);
    private final OfferScreening screening = new OfferScreening(domainPolicy(), overrides, links, mock(com.comprasco.bakeprofit.staging.identity.SecondaryIdentityResolver.class));

    @Test void drogueriaAceptaGtinEquivalenteYRespaldoExactoSinCambiarOferta() {
        var offer = offer("Droguería");
        var target = ScrapeTarget.of(offer.getStore(), offer.getScrapeTarget().getCategory(), "algodón", UnitBase.G, NET_CONTENT, NET_CONTENT);
        ReflectionTestUtils.setField(target, "expectedEan", GTIN_14);
        when(offer.getScrapeTarget()).thenReturn(target);
        when(offer.getNetContent()).thenReturn(null);
        when(offer.getUnitBase()).thenReturn(null);
        var result = screening.screen(offer);
        assertThat(result.isRejected()).isFalse();
        assertThat(result.accepted().netContent()).isEqualByComparingTo(NET_CONTENT);
        assertThat(offer.getNetContent()).isNull();
    }
    @Test void noRespaldaUnRangoNiSobrescribeUnTamanoPublicado() {
        var offer = offer(SUPERMARKET);
        ReflectionTestUtils.setField(offer.getScrapeTarget(), "expectedEan", GTIN_14);
        when(offer.getNetContent()).thenReturn(null);
        assertThat(screening.screen(offer).isRejected()).isTrue();
        when(offer.getNetContent()).thenReturn(new BigDecimal("2500"));
        assertThat(screening.screen(offer).isRejected()).isTrue();
    }
    @Test void rechazaEanDistintoAunqueElTamanoSeaCorrecto() {
        var offer = offer("Droguería");
        ReflectionTestUtils.setField(offer.getScrapeTarget(), "expectedEan", "036000291452");
        assertThat(screening.screen(offer).rejection()).contains("no es el que espera");
    }
    @Test void overrideResuelveContenidoConservandoDatoCrudo() {
        var offer = offer(SUPERMARKET);
        when(offer.getNetContent()).thenReturn(new BigDecimal("8000"));
        var override = mock(EanSizeOverride.class);
        when(override.getNetContent()).thenReturn(NET_CONTENT);
        when(override.getUnitBase()).thenReturn(UnitBase.G);
        when(overrides.findById(GTIN_14)).thenReturn(Optional.of(override));
        assertThat(screening.screen(offer).accepted().netContent()).isEqualByComparingTo("500");
        assertThat(offer.getNetContent()).isEqualByComparingTo("8000");
    }
    @Test void pesoVariableSinAsignacionQuedaParaRevision() {
        var offer = offer(SUPERMARKET);
        when(offer.getEan()).thenReturn("2000123456782");
        assertThat(screening.screen(offer).rejection()).isEqualTo("peso variable sin asignar");
    }
    @Test void pesoVariableUsaSkuYPesoPublicadoParaPrecioPorKilo() {
        var offer = offer(SUPERMARKET);
        when(offer.getEan()).thenReturn("2000123456782");
        when(offer.getStoreSku()).thenReturn("sku-1");
        when(offer.getSaleWeightKg()).thenReturn(new BigDecimal("1.4"));
        when(offer.getPrice()).thenReturn(new BigDecimal("33572"));
        Product product = Product.withIdentity("Pechuga sin piel", offer.getScrapeTarget().getCategory(), null, null, new BigDecimal("1000"), UnitBase.G);
        var link = mock(StoreProductLink.class);
        when(link.getProduct()).thenReturn(product);
        when(links.findByStoreIdAndStoreSku(1L, "sku-1")).thenReturn(Optional.of(link));
        var result = screening.screen(offer);
        assertThat(result.isRejected()).isFalse();
        assertThat(result.accepted().price()).isEqualByComparingTo("23980");
        assertThat(result.accepted().linkedProduct()).isSameAs(product);
        assertThat(offer.getPrice()).isEqualByComparingTo("33572");
        when(offer.getSaleWeightKg()).thenReturn(null);
        assertThat(screening.screen(offer).rejection()).contains("sin peso");
    }
}
