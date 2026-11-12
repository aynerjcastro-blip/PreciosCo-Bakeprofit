package com.comprasco.bakeprofit.staging.resolver;

import static com.comprasco.bakeprofit.staging.StagingFixtures.EAN_13;
import static com.comprasco.bakeprofit.staging.StagingFixtures.GTIN_14;
import static com.comprasco.bakeprofit.staging.StagingFixtures.SUPERMARKET;
import static com.comprasco.bakeprofit.staging.StagingFixtures.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.staging.repository.PendingOfferSize;
import com.comprasco.bakeprofit.staging.entity.ScrapedOffer;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;


@ExtendWith(MockitoExtension.class)
class SizeConflictCheckTest {

    // La oferta de StagingFixtures trae 500 G de la tienda "Tienda", con id 100
    private static final OfferScreening.Accepted ACCEPTED = new OfferScreening.Accepted(GTIN_14, UnitBase.G);
    private static final Long OFFER_ID = 100L;

    @Mock
    private ScrapedOfferRepository offerRepository;

    private SizeConflictCheck check;
    private ScrapedOffer offer;

    private record Size(Long id, BigDecimal netContent, String unitBase, String storeName)
            implements PendingOfferSize {

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public BigDecimal getNetContent() {
            return netContent;
        }

        @Override
        public String getUnitBase() {
            return unitBase;
        }

        @Override
        public String getStoreName() {
            return storeName;
        }
    }

    @BeforeEach
    void setUp() {
        check = new SizeConflictCheck(offerRepository);
        offer = offer(SUPERMARKET);
    }

    private void otherOffers(PendingOfferSize... others) {
        when(offerRepository.findOtherPendingSizes(GTIN_14, OFFER_ID)).thenReturn(List.of(others));
    }

    @Test
    void sinOtrasOfertasNoHayConflicto() {
        otherOffers();

        assertThat(check.check(offer, ACCEPTED)).isEmpty();
        verify(offerRepository, never()).unclassifyConflicting(anyList(), anyString(), any());
    }

    @Test
    void elMismoTamanoConOtraEscalaYOtraTiendaNoEsConflicto() {
        otherOffers(new Size(7L, new BigDecimal("500.00"), "g", "Éxito"));

        assertThat(check.check(offer, ACCEPTED)).isEmpty();
        verify(offerRepository, never()).unclassifyConflicting(anyList(), anyString(), any());
    }

    @Test
    void otroTamanoMarcaLasDemasOfertasYDevuelveElMismoMotivo() {
        otherOffers(new Size(7L, new BigDecimal("500"), "G", "Éxito"),
                new Size(8L, new BigDecimal("25000"), "G", "Jumbo"));

        Optional<String> reason = check.check(offer, ACCEPTED);

        String expected = SizeConflictCheck.SIZE_CONFLICT_REASON.formatted(EAN_13,
                "25 kg en Jumbo; 500 g en Tienda, Éxito");
        assertThat(reason).contains(expected);
        verify(offerRepository).unclassifyConflicting(eq(List.of(7L, 8L)), eq(expected), any());
    }

    @Test
    void otraUnidadEsConflictoAunqueElNumeroCoincida() {
        otherOffers(new Size(7L, new BigDecimal("500"), "ML", "Éxito"));

        assertThat(check.check(offer, ACCEPTED)).isPresent();
    }

    @Test
    void unaOfertaConUnidadDesconocidaNoCuenta() {
        otherOffers(new Size(7L, new BigDecimal("25000"), "lb", "Éxito"));

        assertThat(check.check(offer, ACCEPTED)).isEmpty();
        verify(offerRepository, never()).unclassifyConflicting(anyList(), anyString(), any());
    }
}
