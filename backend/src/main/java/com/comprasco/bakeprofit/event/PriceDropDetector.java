package com.comprasco.bakeprofit.event;

import com.comprasco.bakeprofit.dto.PriceDropAlertDto;
import com.comprasco.bakeprofit.service.StagingIntegrationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Componente funcional encargado de evaluar fluctuaciones de precios, 
 * identificar reducciones reales y disparar las notificaciones correspondientes.
 */
@Component
public class PriceDropDetector {

    private final StagingIntegrationService stagingIntegrationService;

    public PriceDropDetector(StagingIntegrationService stagingIntegrationService) {
        this.stagingIntegrationService = stagingIntegrationService;
    }

    /**
     * Evalúa si el precio actual representa una caída real respecto al precio anterior.
     * Si se cumple la condición, construye la alerta y la despacha al servicio de integración.
     *
     * @      productId Identificador único del producto evaluado.
     * @param productName Nombre descriptivo del producto.
     * @param previousPrice Precio registrado anteriormente.
     * @param currentPrice Precio actual recién escaneado.
     */
    public void processAndNotifyPriceDrop(Long productId, String productName, double previousPrice, double currentPrice) {
        // Validar que el precio actual sea estrictamente menor que el anterior (caída real de precio)
        if (currentPrice < previousPrice) {
            // Construir el DTO inmutable con la información de la caída
            PriceDropAlertDto alert = new PriceDropAlertDto(
                productId, 
                productName, 
                previousPrice, 
                currentPrice, 
                LocalDateTime.now()
            );

            // Sincronizar y enviar la alerta hacia el procesador de staging
            boolean sent = stagingIntegrationService.sendPriceDropAlert(alert);
            
            if (!sent) {
                System.err.println("Falló la sincronización de la caída de precio para el producto " + productId + " con staging.");
            }
        }
    }
}