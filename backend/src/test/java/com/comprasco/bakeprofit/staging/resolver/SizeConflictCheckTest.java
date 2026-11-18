package com.comprasco.bakeprofit.staging.resolver;
import static com.comprasco.bakeprofit.staging.StagingFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;

class SizeConflictCheckTest {
    private final ScrapedOfferRepository repo = mock(ScrapedOfferRepository.class);
    private final OfferScreening screening = mock(OfferScreening.class);
    private final SizeConflictCheck check = new SizeConflictCheck(repo, screening);
    private final OfferScreening.Accepted accepted = new OfferScreening.Accepted(GTIN_14, UnitBase.G, NET_CONTENT, null, PRICE);

    @Test void sinOtrasOfertasNoHayConflicto() {
        assertThat(check.check(offer(SUPERMARKET), accepted)).isEmpty();
    }
    @Test void comparaTamanoEfectivoDelRespaldoYNoElCrudo() {
        var other = offer(SUPERMARKET);
        when(other.getNetContent()).thenReturn(null);
        when(repo.findOtherSizeEvidenceOffers(GTIN_14, 100L)).thenReturn(List.of(other));
        when(screening.screenSizeEvidence(other)).thenReturn(new OfferScreening.Result(accepted, null));
        assertThat(check.check(offer(SUPERMARKET), accepted)).isEmpty();
    }
    @Test void conflictoMarcaLasOtrasOfertas() {
        var other = offer(SUPERMARKET);
        when(other.getId()).thenReturn(101L);
        when(repo.findOtherSizeEvidenceOffers(GTIN_14, 100L)).thenReturn(List.of(other));
        when(repo.findById(101L)).thenReturn(Optional.of(other));
        when(screening.screenSizeEvidence(other)).thenReturn(new OfferScreening.Result(
                new OfferScreening.Accepted(GTIN_14, UnitBase.G, new BigDecimal("1000"), null, PRICE), null));
        String reason = check.check(offer(SUPERMARKET), accepted).orElseThrow();
        assertThat(reason).contains("1 kg", "500 g");
        verify(other).markUnclassified(reason);
    }
    @Test void eanNoEsperadoNoBloqueaOtraTienda() {
        var other = offer(SUPERMARKET);
        when(repo.findOtherSizeEvidenceOffers(GTIN_14, 100L)).thenReturn(List.of(other));
        when(screening.screenSizeEvidence(other)).thenReturn(new OfferScreening.Result(null, "EAN distinto"));
        assertThat(check.check(offer(SUPERMARKET), accepted)).isEmpty();
    }
}
