package com.comprasco.bakeprofit.service;

import com.comprasco.bakeprofit.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class StagingIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(StagingIntegrationService.class);
    private final RestTemplate restTemplate;

    @Value("${staging.processor.url}")
    private String stagingUrl;

    public StagingIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendPriceDropAlert(PriceDropAlertDto alertDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PriceDropAlertDto> requestEntity = new HttpEntity<>(alertDto, headers);
            
            logger.info("Sending price drop alert for product ID: {} to staging processor...", alertDto.getProductId());
            
            ResponseEntity<String> response = restTemplate.postForEntity(stagingUrl, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Price drop alert successfully sent and processed in staging!");
                return true;
            } else {
                logger.warn("Staging processor responded with a non-success status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Critical error connecting price drop detector to staging: {}", e.getMessage());
            return false;
        }
    }
}