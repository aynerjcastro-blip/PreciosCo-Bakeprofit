package com.comprasco.bakeprofit.ai.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.config.AiProperties;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.exception.AiEmptyResponseException;
import com.comprasco.bakeprofit.ai.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cliente HTTP de la API de Gemini (generateContent y streamGenerateContent).
 *
 * <ul>
 * <li>La API key viaja solo en el header {@code x-goog-api-key}, nunca en la
 * URL (las URLs terminan en logs y trazas).</li>
 * <li>Todas las llamadas tienen timeouts para no dejar hilos colgados.</li>
 * <li>Los errores de Gemini se registran en el log y al usuario solo le llega
 * un mensaje genérico.</li>
 * </ul>
 */
@Component
public class GeminiClient {

        private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

        private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
        private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
        /** Tiempo máximo hasta recibir la respuesta (o los headers en streaming). */
        private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;
        private final AiProperties properties;

        public GeminiClient(AiProperties properties) {
                this.httpClient = HttpClient.newBuilder()
                                .connectTimeout(CONNECT_TIMEOUT)
                                .build();
                this.objectMapper = new ObjectMapper();
                this.properties = properties;
        }

        /**
         * Envía una conversación a Gemini y devuelve la respuesta como texto.
         * (Método sin streaming — se mantiene como fallback)
         */
        public String chat(String systemPrompt, List<AiMessage> messages)
                        throws IOException, InterruptedException {

                HttpResponse<String> response = send(
                                "generateContent", systemPrompt, messages,
                                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() != 200) {
                        throw providerError(response.statusCode(), response.body());
                }

                String text = extractText(response.body());
                if (text == null || text.isBlank()) {
                        throw new AiEmptyResponseException();
                }
                return text;
        }

        /**
         * Envía una conversación a Gemini con streaming (SSE).
         * Llama a {@code streamGenerateContent?alt=sse} y emite cada fragmento de
         * texto a medida que llega mediante el callback {@code onChunk}.
         *
         * <p>
         * Si {@code onChunk} lanza una excepción (p. ej. el cliente se
         * desconectó), el stream se cierra y se deja de consumir Gemini.
         * </p>
         *
         * @throws AiEmptyResponseException si Gemini no devolvió ningún texto
         *                                  (p. ej. bloqueado por seguridad)
         * @throws AiProviderException      si Gemini respondió con error
         */
        public void chatStream(String systemPrompt, List<AiMessage> messages,
                        Consumer<String> onChunk) throws IOException, InterruptedException {

                HttpResponse<InputStream> response = send(
                                "streamGenerateContent?alt=sse", systemPrompt, messages,
                                HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                        String errorBody;
                        try (InputStream body = response.body()) {
                                errorBody = new String(body.readAllBytes(), StandardCharsets.UTF_8);
                        }
                        throw providerError(response.statusCode(), errorBody);
                }

                // Gemini con alt=sse devuelve líneas tipo:
                // data: { "candidates": [{ "content": { "parts": [{ "text": "Hola" }] } }] }
                boolean emittedAny = false;
                try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                        continue; // líneas vacías y otros campos SSE
                                }
                                String json = line.substring("data:".length()).trim();
                                if (json.isEmpty()) {
                                        continue;
                                }
                                String text = extractText(json);
                                if (text != null && !text.isEmpty()) {
                                        emittedAny = true;
                                        onChunk.accept(text);
                                }
                        }
                }

                if (!emittedAny) {
                        throw new AiEmptyResponseException();
                }
        }

        // ── Helpers privados ───────────────────────────────────

        private <T> HttpResponse<T> send(String method, String systemPrompt, List<AiMessage> messages,
                        HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {

                var payload = new GeminiApiModel.GenerateRequest(
                                GeminiApiModel.SystemInstruction.of(systemPrompt),
                                toContents(messages),
                                generationConfig());

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + properties.model() + ":" + method))
                                .timeout(REQUEST_TIMEOUT)
                                .header("Content-Type", "application/json")
                                .header("x-goog-api-key", properties.apiKey())
                                .POST(HttpRequest.BodyPublishers.ofString(
                                                objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                                .build();

                try {
                        return httpClient.send(request, handler);
                } catch (HttpTimeoutException e) {
                        throw new AiProviderException("El asistente tardó demasiado en responder. Intenta de nuevo.", e);
                } catch (IOException e) {
                        throw new AiProviderException("No se pudo conectar con el asistente de IA.", e);
                }
        }

        /**
         * Gemini 3 no permite {@code thinkingBudget: 0}; se usa
         * {@code thinkingLevel} (configurable). Si está vacío no se envía nada.
         */
        private GeminiApiModel.GenerationConfig generationConfig() {
                String level = properties.thinkingLevel();
                if (level == null || level.isBlank()) {
                        return null;
                }
                return new GeminiApiModel.GenerationConfig(new GeminiApiModel.ThinkingConfig(level.trim()));
        }

        private AiProviderException providerError(int status, String body) {
                log.error("Gemini respondió {} para el modelo '{}': {}", status, properties.model(), body);
                String userMessage = switch (status) {
                        case 429 -> "El asistente de IA está saturado en este momento. Intenta en unos minutos.";
                        case 400, 404 -> "El asistente de IA está mal configurado. Avisa al administrador.";
                        case 401, 403 -> "El asistente de IA no está autorizado. Avisa al administrador.";
                        default -> "El asistente de IA no está disponible en este momento.";
                };
                return new AiProviderException(status, userMessage);
        }

        private List<GeminiApiModel.Content> toContents(List<AiMessage> messages) {
                return messages.stream()
                                .map(m -> new GeminiApiModel.Content(
                                                m.role(),
                                                m.parts().stream().map(GeminiApiModel.Part::new).toList()))
                                .toList();
        }

        /**
         * Concatena el texto de todas las partes de candidates[0] (excepto las
         * de razonamiento interno). Devuelve null si no hay texto.
         */
        private String extractText(String json) {
                try {
                        JsonNode candidates = objectMapper.readTree(json).path("candidates");
                        if (!candidates.isArray() || candidates.isEmpty()) {
                                return null;
                        }
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (!parts.isArray()) {
                                return null;
                        }
                        StringBuilder sb = new StringBuilder();
                        for (JsonNode part : parts) {
                                if (part.path("thought").asBoolean(false)) {
                                        continue;
                                }
                                JsonNode text = part.get("text");
                                if (text != null && text.isTextual()) {
                                        sb.append(text.asText());
                                }
                        }
                        return sb.isEmpty() ? null : sb.toString();
                } catch (Exception e) {
                        log.debug("Chunk de Gemini inválido, se omite: {}", e.getMessage());
                        return null;
                }
        }
}
