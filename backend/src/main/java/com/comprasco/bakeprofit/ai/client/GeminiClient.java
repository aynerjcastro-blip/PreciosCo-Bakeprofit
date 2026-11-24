package com.comprasco.bakeprofit.ai.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.comprasco.bakeprofit.ai.config.AiProperties;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.exception.AiEmptyResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GeminiClient {

        private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

        private final RestClient restClient;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final AiProperties properties;

        public GeminiClient(AiProperties properties) {
                this.restClient = RestClient.create();
                this.httpClient = HttpClient.newHttpClient();
                this.objectMapper = new ObjectMapper();
                this.properties = properties;
        }

        /**
         * Envía una conversación a Gemini y devuelve la respuesta como texto.
         * (Método original sin streaming — se mantiene como fallback)
         */
        public String chat(String systemPrompt, List<AiMessage> messages) {

                List<GeminiApiModel.Content> contents = toContents(messages);

                var request = new GeminiApiModel.GenerateRequest(
                                new GeminiApiModel.SystemInstruction(
                                                List.of(new GeminiApiModel.Part(systemPrompt))),
                                contents);

                String url = BASE_URL + properties.model() + ":generateContent?key=" + properties.apiKey();

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

        /**
         * Envía una conversación a Gemini con streaming (SSE).
         * Llama al endpoint {@code streamGenerateContent} y emite cada fragmento
         * de texto a medida que llega, mediante el callback {@code onChunk}.
         *
         * <p>
         * Gemini devuelve un array JSON (no SSE estándar), donde cada elemento
         * es un {@code GenerateResponse} parcial. Este método lee línea por línea
         * y extrae el texto de cada chunk.
         *
         * @param systemPrompt prompt del sistema con catálogo inyectado
         * @param messages     historial de la conversación
         * @param onChunk      callback invocado con cada fragmento de texto
         * @throws IOException          si falla la conexión HTTP
         * @throws InterruptedException si el hilo es interrumpido
         */
        public void chatStream(String systemPrompt, List<AiMessage> messages,
                        Consumer<String> onChunk) throws IOException, InterruptedException {

                List<GeminiApiModel.Content> contents = toContents(messages);

                var payload = new GeminiApiModel.GenerateRequest(
                                new GeminiApiModel.SystemInstruction(
                                                List.of(new GeminiApiModel.Part(systemPrompt))),
                                contents);

                String url = BASE_URL + properties.model()
                                + ":streamGenerateContent?alt=sse&key=" + properties.apiKey();

                String jsonBody = objectMapper.writeValueAsString(payload);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                                .build();

                // Consumir la respuesta como InputStream para leer línea a línea
                HttpResponse<InputStream> response = httpClient.send(
                                httpRequest, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                        String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                        throw new IOException("Gemini streaming error (%d): %s"
                                        .formatted(response.statusCode(), errorBody));
                }

                // Gemini con alt=sse devuelve líneas tipo:
                // data: { "candidates": [{ "content": { "parts": [{ "text": "Hola" }] } }] }
                //
                // Leemos línea por línea y extraemos el texto de cada "data:" event.
                try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data: ")) {
                                        continue; // saltar líneas vacías y otros prefijos SSE
                                }

                                String json = line.substring("data: ".length()).trim();
                                if (json.isEmpty()) {
                                        continue;
                                }

                                String text = extractText(json);
                                if (text != null && !text.isEmpty()) {
                                        onChunk.accept(text);
                                }
                        }
                }
        }

        // ── Helpers privados ───────────────────────────────────

        private List<GeminiApiModel.Content> toContents(List<AiMessage> messages) {
                return messages.stream()
                                .map(m -> new GeminiApiModel.Content(
                                                m.role(),
                                                m.parts().stream().map(GeminiApiModel.Part::new).toList()))
                                .toList();
        }

        /**
         * Extrae candidates[0].content.parts[0].text de un JSON de Gemini.
         * Devuelve null si no encuentra texto.
         */
        private String extractText(String json) {
                try {
                        JsonNode root = objectMapper.readTree(json);
                        JsonNode candidates = root.path("candidates");
                        if (candidates.isMissingNode() || candidates.isEmpty()) {
                                return null;
                        }
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isMissingNode() || parts.isEmpty()) {
                                return null;
                        }
                        return parts.get(0).path("text").asText(null);
                } catch (Exception e) {
                        return null; // chunk inválido — saltar sin romper el stream
                }
        }
}