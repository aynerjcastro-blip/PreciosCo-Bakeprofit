package com.comprasco.bakeprofit.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración del módulo de IA.
 * Se leen del prefijo {@code preciosco.ai} en application.properties
 * o variables de entorno (PRECIOSCO_AI_API_KEY, PRECIOSCO_AI_MODEL).
 */
@ConfigurationProperties(prefix = "preciosco.ai")
public record AiProperties(
        String apiKey,
        String model) {
}