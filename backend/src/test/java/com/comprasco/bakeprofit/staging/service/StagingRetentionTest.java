package com.comprasco.bakeprofit.staging.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.comprasco.bakeprofit.staging.StagingFixtures;
import com.comprasco.bakeprofit.staging.entity.OfferStatus;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;
import com.comprasco.bakeprofit.staging.service.StagingRetention;

@ExtendWith(MockitoExtension.class)
class StagingRetentionTest {

    @Mock
    private ScrapedOfferRepository offerRepository;

    @Test
    void borraProcesadasYSinClasificarConSusPlazosYNuncaPendientes() {
        StagingRetention retention = new StagingRetention(offerRepository, StagingFixtures.properties());
        when(offerRepository.deleteByStatusMarkedBefore(eq(OfferStatus.PROCESSED), any())).thenReturn(4);
        when(offerRepository.deleteByStatusMarkedBefore(eq(OfferStatus.UNCLASSIFIED), any())).thenReturn(1);
        ArgumentCaptor<LocalDateTime> processedCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> unclassifiedCutoff = ArgumentCaptor.forClass(LocalDateTime.class);

        StagingRetention.Purged purged = retention.purge();

        assertThat(purged).isEqualTo(new StagingRetention.Purged(4, 1));
        verify(offerRepository).deleteByStatusMarkedBefore(eq(OfferStatus.PROCESSED), processedCutoff.capture());
        verify(offerRepository).deleteByStatusMarkedBefore(eq(OfferStatus.UNCLASSIFIED), unclassifiedCutoff.capture());
        verify(offerRepository, never()).deleteByStatusMarkedBefore(eq(OfferStatus.PENDING), any());
        LocalDateTime now = LocalDateTime.now();
        assertThat(processedCutoff.getValue()).isCloseTo(now.minusDays(2), within(1, ChronoUnit.MINUTES));
        assertThat(unclassifiedCutoff.getValue()).isCloseTo(now.minusDays(7), within(1, ChronoUnit.MINUTES));
    }
}
