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

/**
 * Servicio encargado de gestionar la comunicación HTTP hacia el entorno de Staging,
 * permitiendo sincronizar las alertas de ofertas y cambios de precios detectados.
 */
@Service
public class StagingIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(StagingIntegrationService.class);
    private final RestTemplate restTemplate;

    @Value("${staging.processor.url:}")
    private String stagingUrl;

    public StagingIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envía una alerta de caída de precio empaquetada en un DTO hacia el procesador de staging vía POST.
     * 
     * @param alertDto Objeto que contiene los detalles de la caída de precio del producto.
     * @return true si la solicitud fue exitosa (código HTTP 2xx), false en caso contrario.
     */
    public boolean sendPriceDropAlert(PriceDropAlertDto alertDto) {
        try {
            // Configurar los encabezados de la petición HTTP especificando formato JSON
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Crear la entidad HTTP combinando el DTO con los encabezados
            HttpEntity<PriceDropAlertDto> requestEntity = new HttpEntity<>(alertDto, headers);
            
            logger.info("Enviando alerta de caída de precio para el producto ID: {} hacia el procesador de staging...", alertDto.productId());
            
            // Ejecutar la petición POST hacia la URL de staging configurada
            ResponseEntity<String> response = restTemplate.postForEntity(stagingUrl, requestEntity, String.class);
            
            // Validar si la respuesta del servidor de staging fue exitosa
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("¡Alerta de caída de precio enviada y procesada exitosamente en staging!");
                return true;
            } else {
                logger.warn("El procesador de staging respondió con un estado no exitoso: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error crítico al conectar el detector de caídas de precio con staging: {}", e.getMessage());
            return false;
        }
    }
}