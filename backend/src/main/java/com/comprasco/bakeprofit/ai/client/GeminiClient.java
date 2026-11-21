package com.comprasco.bakeprofit.ai.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.comprasco.bakeprofit.ai.config.AiProperties;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.exception.AiEmptyResponseException;

@Component
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final AiProperties properties;

    public GeminiClient(AiProperties properties) {
        this.restClient = RestClient.create();
        this.properties = properties;
    }

    /**
     * Envía una conversación a Gemini y devuelve la respuesta como texto.
     *
     * @param systemPrompt instrucciones del sistema (prompt de sección + catálogo)
     * @param messages     historial de la conversación en formato público
     *                     (AiMessage)
     * @return texto de la respuesta del modelo
     */
    public String chat(String systemPrompt, List<AiMessage> messages) {

        // Convertir AiMessage (público) → Content (interno de Gemini)
        List<GeminiApiModel.Content> contents = messages.stream()
                .map(m -> new GeminiApiModel.Content(
                        m.role(),
                        m.parts().stream().map(GeminiApiModel.Part::new).toList()))
                .toList();

        var request = new GeminiApiModel.GenerateRequest(
                new GeminiApiModel.SystemInstruction(
                        List.of(new GeminiApiModel.Part(systemPrompt))),
                contents);

        String url = BASE_URL + properties.model() + ":generateContent";

        GeminiApiModel.GenerateResponse response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", properties.apiKey())
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(GeminiApiModel.GenerateResponse.class);

        String text = response.text();
        if (text == null) {
            throw new AiEmptyResponseException("Gemini no devolvió respuesta válida");
        }
        return text;
    }
}