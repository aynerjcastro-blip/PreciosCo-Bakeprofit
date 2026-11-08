package com.comprasco.bakeprofit.event;

import com.comprasco.bakeprofit.dto.PriceDropAlertDto;
import com.comprasco.bakeprofit.service.StagingIntegrationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Componente evaluador de ofertas y caídas de precio sobre el nuevo modelo de catálogo.
 * Valida que sólo las reducciones de precio del producto exacto disparen alertas,
 * ignorando cambios de tamaño o presentación (SCRUM-267).
 */
@Component
public class PriceDropDetector {

    private final StagingIntegrationService stagingIntegrationService;

    public PriceDropDetector(StagingIntegrationService stagingIntegrationService) {
        this.stagingIntegrationService = stagingIntegrationService;
    }

    /**
     * Evalúa la fluctuación de precio y tamaño de un producto.
     * 
     * @param productId ID del producto exacto.
     * @param productName Nombre del producto.
     * @param previousPrice Precio anterior.
     * @param currentPrice Precio actual.
     * @param previousNetContent Contenido neto anterior.
     * @param currentNetContent Contenido neto actual.
     */
    public void evaluateProductChange(Long productId, String productName, double previousPrice, double currentPrice, double previousNetContent, double currentNetContent) {
        
        // REGLA (SCRUM-267): Un cambio de tamaño (netContent) NO genera ninguna alerta de precio.
        if (previousNetContent != currentNetContent) {
            System.out.println("Cambio de tamaño detectado para el producto " + productId + ". Ignorando alerta de precio.");
            return;
        }

        // REGLA (SCRUM-267): Un precio menor del mismo producto exacto genera el aviso.
        if (currentPrice < previousPrice) {
            PriceDropAlertDto alert = new PriceDropAlertDto(
                productId, 
                productName, 
                previousPrice, 
                currentPrice, 
                LocalDateTime.now()
            );

            boolean sent = stagingIntegrationService.sendPriceDropAlert(alert);
            
            if (!sent) {
                System.err.println("Falló la sincronización de la alerta de precio para el producto exacto: " + productId);
            }
        }
    }
}