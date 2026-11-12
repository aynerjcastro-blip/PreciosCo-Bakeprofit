package com.comprasco.bakeprofit.price.service;

import static com.comprasco.bakeprofit.staging.StagingFixtures.PRICE;
import static com.comprasco.bakeprofit.staging.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.staging.StagingFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.price.entity.Price;
import com.comprasco.bakeprofit.price.repository.PriceRepository;
import com.comprasco.bakeprofit.staging.StagingFixtures;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;

@ExtendWith(MockitoExtension.class)
class PriceRecorderTest {

    private static final Long PRODUCT_ID = 7L;

    @Mock
    private PriceRepository priceRepository;

    private PriceRecorder recorder;
    private Product product;
    private ScrapedOffer offer;

    @BeforeEach
    void setUp() {
        recorder = new PriceRecorder(priceRepository);
        product = new Product();
        product.setId(PRODUCT_ID);
        offer = offer(SUPERMARKET);
    }

    private void latestIs(Optional<Price> latest) {
        when(priceRepository.findFirstByProductIdAndStoreIdOrderByRegistrationDateDescIdDesc(PRODUCT_ID, 1L))
                .thenReturn(latest);
    }

    @Test
    void precioRepetidoSoloRefrescaElPeriodoVigente() {
        Price latest = Price.observed(product, offer.getStore(), new BigDecimal("4500.00"), "x", null, null);
        latestIs(Optional.of(latest));

        Optional<Price> inserted = recorder.record(product, offer);

        assertThat(inserted).isEmpty();
        assertThat(latest.getProductUrl()).isEqualTo(StagingFixtures.PRODUCT_URL);
        assertThat(latest.getImageUrl()).isEqualTo(StagingFixtures.IMAGE_URL);
        verify(priceRepository, never()).save(any());
    }

    @Test
    void precioDistintoAbreUnaFilaNuevaConLosDatosDeLaOferta() {
        latestIs(Optional.of(Price.observed(product, offer.getStore(), new BigDecimal("4900"), "x", null, null)));
        when(priceRepository.save(any(Price.class))).thenAnswer(call -> call.getArgument(0));

        Price inserted = recorder.record(product, offer).orElseThrow();

        assertThat(inserted.getValue()).isEqualByComparingTo(PRICE);
        assertThat(inserted.getSource()).isEqualTo(StagingFixtures.OFFER_NAME);
        assertThat(inserted.getProductUrl()).isEqualTo(StagingFixtures.PRODUCT_URL);
        assertThat(inserted.getImageUrl()).isEqualTo(StagingFixtures.IMAGE_URL);
        assertThat(inserted.getProduct()).isSameAs(product);
        assertThat(inserted.getStore()).isSameAs(offer.getStore());
    }

    @Test
    void sinPrecioAnteriorInserta() {
        latestIs(Optional.empty());
        when(priceRepository.save(any(Price.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(recorder.record(product, offer)).isPresent();
    }
}
