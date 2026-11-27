package com.comprasco.bakeprofit.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración del módulo de IA.
 * Se leen del prefijo {@code preciosco.ai} en application.properties
 * o variables de entorno (AI_API_KEY, AI_MODEL, AI_THINKING_LEVEL).
 *
 * @param apiKey         clave de la API de Gemini
 * @param model          modelo de Gemini (p. ej. {@code gemini-3.5-flash-lite})
 * @param thinkingLevel  nivel de razonamiento de los modelos Gemini 3
 *                       ({@code minimal}, {@code low}, {@code medium},
 *                       {@code high}). Vacío = no se envía y Gemini usa su
 *                       valor por defecto.
 */
@ConfigurationProperties(prefix = "preciosco.ai")
public record AiProperties(
        String apiKey,
        String model,
        String thinkingLevel) {
}
