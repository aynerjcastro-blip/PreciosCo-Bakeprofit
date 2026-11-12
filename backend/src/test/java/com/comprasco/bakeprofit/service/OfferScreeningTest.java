package com.comprasco.bakeprofit.service;

import static com.comprasco.bakeprofit.service.StagingFixtures.SPARE_PARTS;
import static com.comprasco.bakeprofit.service.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.service.StagingFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.comprasco.bakeprofit.entity.ScrapeTarget;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.entity.UnitBase;
import com.comprasco.bakeprofit.util.GtinVerdict;

class OfferScreeningTest {

    private final OfferScreening screening = new OfferScreening(StagingFixtures.domainPolicy());

    @Test
    void aceptaUnaOfertaValidaConElEanNormalizadoYLaUnidadInterpretada() {
        OfferScreening.Result result = screening.screen(offer(SUPERMARKET));

        assertThat(result.isRejected()).isFalse();
        assertThat(result.accepted().gtin()).isEqualTo(StagingFixtures.GTIN_14);
        assertThat(result.accepted().unitBase()).isEqualTo(UnitBase.G);
    }

    @Test
    void sinObjetivoDeBusqueda() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getScrapeTarget()).thenReturn(null);

        assertThat(screening.screen(offer).rejection()).isEqualTo(OfferScreening.NO_TARGET_REASON);
    }

    // Un código por veredicto distinto de COMPARABLE; el motivo es el mensaje del veredicto
    @ParameterizedTest
    @CsvSource({
        "7702552000142,  INVALID",
        "0000000000000,  PLACEHOLDER",
        "2000123456782,  STORE_INTERNAL",
        "90012345678908, VARIABLE_MEASURE"
    })
    void cadaVeredictoGtinDistintoDeComparableDejaSuMensaje(String ean, GtinVerdict verdict) {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getEan()).thenReturn(ean);

        assertThat(screening.screen(offer).rejection())
                .isEqualTo(verdict.rejectionMessage(ean).orElseThrow());
    }

    @Test
    void sinEan() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getEan()).thenReturn(null);

        assertThat(screening.screen(offer).rejection()).isEqualTo(OfferScreening.NO_EAN_REASON);
    }

    @Test
    void sinContenidoNeto() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getNetContent()).thenReturn(null);

        assertThat(screening.screen(offer).rejection()).isEqualTo(OfferScreening.NO_CONTENT_REASON);
    }

    @Test
    void unidadDesconocida() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getUnitBase()).thenReturn("LB");

        assertThat(screening.screen(offer).rejection())
                .isEqualTo(OfferScreening.UNKNOWN_UNIT_REASON.formatted("LB"));
    }

    @Test
    void laUnidadSeInterpretaSinDistinguirMayusculas() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getUnitBase()).thenReturn(" g ");

        assertThat(screening.screen(offer).isRejected()).isFalse();
    }

    @Test
    void fueraDelRangoDelObjetivo() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getNetContent()).thenReturn(new BigDecimal("2500"));

        assertThat(screening.screen(offer).rejection())
                .isEqualTo(OfferScreening.OUT_OF_TARGET_REASON.formatted("2500", "G", "arroz", "G", "400", "1000"));
    }

    @Test
    void otraUnidadQueLaDelObjetivo() {
        ScrapedOffer offer = offer(SUPERMARKET);
        when(offer.getUnitBase()).thenReturn("ML");

        assertThat(screening.screen(offer).rejection()).startsWith("El tamaño 500 ML no cumple");
    }

    @Test
    void laCategoriaDelObjetivoDebeSerUnaHoja() {
        ScrapedOffer offer = offer(SUPERMARKET);
        ScrapeTarget onRoot = ScrapeTarget.of(offer.getStore(), offer.getStore().getCategory(), "arroz",
                UnitBase.G, null, null);
        when(offer.getScrapeTarget()).thenReturn(onRoot);

        assertThat(screening.screen(offer).rejection())
                .isEqualTo(OfferScreening.INVALID_CATEGORY_REASON.formatted(SUPERMARKET));
    }

    @Test
    void repuestosQuedaPendienteDeIdentidadPorReferencia() {
        assertThat(screening.screen(offer(SPARE_PARTS)).rejection())
                .isEqualTo(OfferScreening.PENDING_IDENTITY_REASON);
    }
}
