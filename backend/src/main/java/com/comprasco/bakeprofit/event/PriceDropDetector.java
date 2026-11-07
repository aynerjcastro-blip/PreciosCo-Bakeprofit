package com.comprasco.bakeprofit.event;

import com.comprasco.bakeprofit.dto.*;
import com.comprasco.bakeprofit.service.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PriceDropDetector {

    private final StagingIntegrationService stagingIntegrationService;

    public PriceDropDetector(StagingIntegrationService stagingIntegrationService) {
        this.stagingIntegrationService = stagingIntegrationService;
    }

    /**
     * Evaluates a detected price drop and dispatches it to the staging processor.
     */
    public void processAndNotifyPriceDrop(Long productId, String productName, double previousPrice, double currentPrice) {
        // Validate that it is indeed a price drop
        if (currentPrice < previousPrice) {
            PriceDropAlertDto alert = new PriceDropAlertDto(
                productId, 
                productName, 
                previousPrice, 
                currentPrice, 
                LocalDateTime.now()
            );

            // Connect and send to staging processor
            boolean sent = stagingIntegrationService.sendPriceDropAlert(alert);
            
            if (!sent) {
                System.err.println("Failed to synchronize price drop for product " + productId + " with staging.");
            }
        }
    }
}