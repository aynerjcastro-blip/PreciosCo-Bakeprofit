package com.comprasco.bakeprofit.service;

import static com.comprasco.bakeprofit.service.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.service.StagingFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.comprasco.bakeprofit.entity.Price;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.entity.UnitBase;
import com.comprasco.bakeprofit.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.repository.ScrapedOfferRepository;
import com.comprasco.bakeprofit.service.OfferIngestionService.Outcome;
import com.comprasco.bakeprofit.service.OfferIngestionService.Result;

@ExtendWith(MockitoExtension.class)
class OfferIngestionServiceTest {

    private static final Long OFFER_ID = 100L;
    private static final OfferScreening.Accepted ACCEPTED =
            new OfferScreening.Accepted(StagingFixtures.GTIN_14, UnitBase.G);

    @Mock
    private ScrapedOfferRepository offerRepository;
    @Mock
    private OfferScreening screening;
    @Mock
    private OfferProductResolver productResolver;
    @Mock
    private PriceRecorder priceRecorder;

    private OfferIngestionService service;
    private ScrapedOffer offer;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new OfferIngestionService(offerRepository, screening, productResolver, priceRecorder);
        offer = offer(SUPERMARKET);
        product = new Product();
        product.setId(7L);
        when(offerRepository.lockById(OFFER_ID)).thenReturn(Optional.of(offer));
    }

    private void passesUntilPrice() {
        when(screening.screen(offer)).thenReturn(new OfferScreening.Result(ACCEPTED, null));
        when(productResolver.resolve(offer, ACCEPTED)).thenReturn(OfferProductResolver.Match.of(product));
    }

    @Test
    void ofertaQueYaNoEstaPendienteSeOmite() {
        when(offer.isPending()).thenReturn(false);

        assertThat(service.process(OFFER_ID).result()).isEqualTo(Result.SKIPPED);
        verifyNoInteractions(screening, productResolver, priceRecorder);
    }

    @Test
    void unRechazoDeLosFiltrosLaMarcaSinClasificarSinTocarElCatalogo() {
        when(screening.screen(offer)).thenReturn(OfferScreening.Result.rejected("sin objetivo de búsqueda"));

        Outcome outcome = service.process(OFFER_ID);

        assertThat(outcome.result()).isEqualTo(Result.UNCLASSIFIED);
        verify(offer).markUnclassified("sin objetivo de búsqueda");
        verifyNoInteractions(productResolver, priceRecorder);
    }

    @Test
    void unRechazoDelProductoLaMarcaSinClasificarSinRegistrarPrecio() {
        when(screening.screen(offer)).thenReturn(new OfferScreening.Result(ACCEPTED, null));
        when(productResolver.resolve(offer, ACCEPTED)).thenReturn(OfferProductResolver.Match.rejected("otro tamaño"));

        assertThat(service.process(OFFER_ID).result()).isEqualTo(Result.UNCLASSIFIED);
        verify(offer).markUnclassified("otro tamaño");
        verifyNoInteractions(priceRecorder);
    }

    @Test
    void precioNuevoMarcaProcesadaYDevuelveElEventoDelPrecio() {
        passesUntilPrice();
        Price inserted = Price.observed(product, offer.getStore(), StagingFixtures.PRICE, "x", null, null);
        ReflectionTestUtils.setField(inserted, "id", 55L);
        when(priceRecorder.record(product, offer)).thenReturn(Optional.of(inserted));

        Outcome outcome = service.process(OFFER_ID);

        assertThat(outcome.result()).isEqualTo(Result.NEW_PRICE);
        assertThat(outcome.event()).isEqualTo(new PriceRegisteredEvent(55L, 7L, 1L));
        verify(offer).markProcessed();
    }

    @Test
    void precioRepetidoMarcaProcesadaSinEvento() {
        passesUntilPrice();
        when(priceRecorder.record(product, offer)).thenReturn(Optional.empty());

        Outcome outcome = service.process(OFFER_ID);

        assertThat(outcome.result()).isEqualTo(Result.SAME_PRICE);
        assertThat(outcome.hasEvent()).isFalse();
        verify(offer).markProcessed();
        verify(offer, never()).markUnclassified(any());
    }
}
