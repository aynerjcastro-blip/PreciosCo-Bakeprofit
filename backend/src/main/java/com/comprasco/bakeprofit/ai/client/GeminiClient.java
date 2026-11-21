package com.comprasco.bakeprofit.ai.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.comprasco.bakeprofit.ai.config.AiProperties;
import com.comprasco.bakeprofit.ai.exception.AiEmptyResponseException;

/**
 * Cliente HTTP que llama a la API REST de Gemini (generateContent).
 *
 * <p>
 * Responsabilidad única: armar el request, hacer el POST,
 * devolver el texto de la respuesta. No sabe nada de secciones,
 * productos ni prompts — eso es trabajo de otras clases.
 * </p>
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final AiProperties props;

    public GeminiClient(AiProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Envía una conversación a Gemini y devuelve el texto de la respuesta.
     *
     * @param systemPrompt instrucciones del sistema (el "rol" del asistente)
     * @param messages     historial de la conversación (role: "user" o "model")
     * @return el texto generado por el modelo
     * @throws AiEmptyResponseException si Gemini no devuelve texto
     */
    public String chat(String systemPrompt,
            List<GeminiApiModel.Content> messages) {

        var request = new GeminiApiModel.GenerateRequest(
                GeminiApiModel.SystemInstruction.of(systemPrompt),
                messages);

        log.debug("Llamando a Gemini modelo={} mensajes={}", props.model(), messages.size());

        var response = restClient.post()
                .uri("{model}:generateContent", props.model())
                .header("x-goog-api-key", props.apiKey())
                .body(request)
                .retrieve()
                .body(GeminiApiModel.GenerateResponse.class);

        if (response == null) {
            throw new AiEmptyResponseException();
        }

        String text = response.text();
        log.debug("Respuesta de Gemini recibida, longitud={}", text.length());
        return text;
    }
}