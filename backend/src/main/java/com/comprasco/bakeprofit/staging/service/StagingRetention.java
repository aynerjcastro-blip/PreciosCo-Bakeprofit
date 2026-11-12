package com.comprasco.bakeprofit.staging.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.staging.config.StagingProperties;
import com.comprasco.bakeprofit.staging.entity.OfferStatus;
import com.comprasco.bakeprofit.staging.repository.ScrapedOfferRepository;

/**
 * Borra de {@code scraped_offers} las ofertas ya resueltas. Las PROCESSED sobran porque
 * su dato ya está en {@code prices}; las UNCLASSIFIED se guardan más tiempo para poder
 * revisar por qué se rechazaron. Las PENDING nunca se borran.
 */
@Service
@Transactional(readOnly = true)
public class StagingRetention {

    private static final Logger log = LoggerFactory.getLogger(StagingRetention.class);

    private final ScrapedOfferRepository offerRepository;
    private final StagingProperties properties;

    public StagingRetention(ScrapedOfferRepository offerRepository, StagingProperties properties) {
        this.offerRepository = offerRepository;
        this.properties = properties;
    }

    public record Purged(int processed, int unclassified) {
    }

    @Transactional
    public Purged purge() {
        LocalDateTime now = LocalDateTime.now();
        int processed = offerRepository.deleteByStatusMarkedBefore(
                OfferStatus.PROCESSED, now.minus(properties.processedRetention()));
        int unclassified = offerRepository.deleteByStatusMarkedBefore(
                OfferStatus.UNCLASSIFIED, now.minus(properties.unclassifiedRetention()));
        log.info("Retención del staging: {} PROCESSED y {} UNCLASSIFIED borradas", processed, unclassified);
        return new Purged(processed, unclassified);
    }
}
