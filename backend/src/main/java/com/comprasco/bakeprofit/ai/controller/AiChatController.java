package com.comprasco.bakeprofit.ai.controller;

import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.dto.AiQuotaResponse;
import com.comprasco.bakeprofit.ai.exception.AiEmptyResponseException;
import com.comprasco.bakeprofit.ai.exception.AiProviderException;
import com.comprasco.bakeprofit.ai.service.AiChatService;
import com.comprasco.bakeprofit.ai.service.AiChatService.ClientGoneException;
import com.comprasco.bakeprofit.ai.service.AiChatService.PreparedChat;
import com.comprasco.bakeprofit.ai.service.AiQuotaService;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Endpoints del asistente de IA.
 *
 * <p>
 * Los endpoints de streaming validan la petición y reservan la cuota
 * <b>antes</b> de escribir la respuesta. Así los errores de validación (400) y
 * de cuota (429) llegan como respuestas HTTP JSON normales (vía
 * {@code GlobalExceptionHandler}) y no como un evento SSE con estado 200.
 * </p>
 *
 * <p>
 * No se declara {@code produces = text/event-stream} en el mapping: si se
 * declarara, Spring no podría serializar el JSON de error y respondería 406.
 * </p>
 *
 * <p>
 * Eventos SSE emitidos:
 * </p>
 * <ul>
 * <li>{@code event: chunk} → fragmento de texto: {@code {"text":"..."}}</li>
 * <li>{@code event: done} → fin del stream con info de cuota</li>
 * <li>{@code event: error} → error durante el stream: {@code {"message":"..."}}</li>
 * </ul>
 */
@Tag(name = "Integracion de IA")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private static final String GENERIC_ERROR = "El asistente de IA no pudo responder. Intenta de nuevo.";

    private final AiChatService aiChatService;
    private final AiQuotaService quotaService;
    private final ObjectMapper objectMapper;

    public AiChatController(AiChatService aiChatService,
            AiQuotaService quotaService) {
        this.aiChatService = aiChatService;
        this.quotaService = quotaService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Endpoint clásico (sin streaming) — se mantiene como fallback.
     */
    @PostMapping("/chat")
    @Operation(summary = "Enviar mensaje al chat IA con control de cuota")
    public ResponseEntity<AiChatResponse> chat(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request) throws IOException, InterruptedException {

        return ResponseEntity.ok(aiChatService.chat(request, user));
    }

    /**
     * Endpoint con streaming SSE — el frontend recibe fragmentos en tiempo real.
     */
    @PostMapping("/chat/stream")
    @Operation(summary = "Enviar mensaje al chat IA con streaming SSE")
    public void chatStream(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request,
            HttpServletResponse response) throws IOException {

        // Puede lanzar 400/429: todavía no se escribió nada en la respuesta
        PreparedChat prepared = aiChatService.prepareChat(request, user);
        streamTo(response, prepared);
    }

    /**
     * Cotización de lista de productos con streaming SSE (HU-37, SCRUM-324).
     *
     * <p>
     * El usuario envía su lista de compras en el campo {@code message}. Para
     * usuarios FREE, el mensaje no puede superar los 200 caracteres (RF60).
     * El campo {@code section} se ignora (siempre es supermercados).
     * </p>
     */
    @PostMapping("/supermercados/cotizar")
    @Operation(summary = "Cotizar lista de productos de supermercado con IA (streaming SSE)")
    public void cotizarSupermercados(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request,
            HttpServletResponse response) throws IOException {

        PreparedChat prepared = aiChatService.prepareCotizacion(request, user);
        streamTo(response, prepared);
    }

    /**
     * Receta con ingredientes vinculados al catálogo, con streaming SSE
     * (SCRUM-325, RF30).
     *
     * <p>
     * El usuario escribe el plato en {@code message} ("ajiaco para 4"). Usa la
     * cuota mensual de recetas (no gasta consultas). Antes del stream se extraen
     * los ingredientes y se buscan en el catálogo; el evento {@code done} trae
     * esa disponibilidad ({@link com.comprasco.bakeprofit.ai.dto.AiRecipeResponse}).
     * El campo {@code section} se ignora.
     * </p>
     */
    @PostMapping("/supermercados/receta")
    @Operation(summary = "Pedir una receta con ingredientes del catálogo de supermercados (streaming SSE)")
    public void receta(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request,
            HttpServletResponse response) throws IOException, InterruptedException {

        PreparedChat prepared = aiChatService.prepareReceta(request, user);
        streamTo(response, prepared);
    }

    @GetMapping("/quota")
    @Operation(summary = "Consultar cuota actual de consultas y recetas IA del usuario")
    public ResponseEntity<AiQuotaResponse> getQuota(@CurrentUser User user) {
        return ResponseEntity.ok(new AiQuotaResponse(
                user.getAiQueriesUsed(),
                quotaService.limitFor(user),
                user.getPlan().name(),
                user.getAiRecipesUsed(),
                quotaService.recipeLimitFor(user)));
    }

    // ── Streaming ─────────────────────────────────────────

    private void streamTo(HttpServletResponse response, PreparedChat prepared) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // Desactivar buffering de proxies

        PrintWriter writer = response.getWriter();

        try {
            Object donePayload = aiChatService.stream(prepared, chunk -> {
                try {
                    send(writer, "chunk", "{\"text\":" + objectMapper.writeValueAsString(chunk) + "}");
                } catch (IOException e) {
                    throw new ClientGoneException();
                }
            });
            send(writer, "done", objectMapper.writeValueAsString(donePayload));

        } catch (ClientGoneException e) {
            log.debug("Cliente desconectado durante el streaming IA");
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // Solo mensajes pensados para el usuario; el resto queda en el log
            String message = (e instanceof AiProviderException || e instanceof AiEmptyResponseException)
                    ? e.getMessage()
                    : GENERIC_ERROR;
            log.error("Error en streaming IA: {}", e.getMessage(), e);
            try {
                send(writer, "error", "{\"message\":" + objectMapper.writeValueAsString(message) + "}");
            } catch (IOException ignored) {
                // Cliente ya desconectado
            }
        }
    }

    /**
     * Escribe un evento SSE. {@link PrintWriter} no lanza excepciones, así que se
     * revisa {@code checkError()} para detectar que el cliente se fue.
     */
    private static void send(PrintWriter writer, String event, String data) throws IOException {
        writer.write("event: " + event + "\ndata: " + data + "\n\n");
        writer.flush();
        if (writer.checkError()) {
            throw new IOException("Cliente desconectado");
        }
    }
}
