package com.comprasco.bakeprofit.ai.controller;

import java.io.IOException;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.dto.AiQuotaResponse;
import com.comprasco.bakeprofit.ai.service.AiChatService;
import com.comprasco.bakeprofit.ai.service.AiQuotaService;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Tag(name = "Integracion de IA")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

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
            @Valid @RequestBody AiChatRequest request) {

        AiChatResponse response = aiChatService.chat(request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint con streaming SSE — el frontend recibe fragmentos en tiempo real.
     *
     * <p>
     * Escribe directamente en el {@code HttpServletResponse} para evitar
     * problemas de sincronización con {@code SseEmitter} + virtual threads.
     * </p>
     *
     * <p>
     * Eventos emitidos:
     * </p>
     * <ul>
     * <li>{@code event: chunk} → fragmento de texto: {@code {"text":"..."}}</li>
     * <li>{@code event: done} → fin del stream con info de cuota</li>
     * <li>{@code event: error} → error durante el stream</li>
     * </ul>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Enviar mensaje al chat IA con streaming SSE")
    public void chatStream(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request,
            HttpServletResponse response) throws IOException {

        // Configurar la respuesta HTTP como SSE
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no"); // Desactivar buffering de proxies

        PrintWriter writer = response.getWriter();

        try {
            AiChatResponse quotaInfo = aiChatService.chatStream(
                    request,
                    user,
                    chunk -> {
                        try {
                            String escapedText = objectMapper.writeValueAsString(chunk);
                            writer.write("event: chunk\ndata: {\"text\":" + escapedText + "}\n\n");
                            writer.flush();
                        } catch (Exception e) {
                            log.debug("Error enviando chunk al cliente: {}", e.getMessage());
                        }
                    });

            // Enviar evento final con la info de cuota
            writer.write("event: done\ndata: " + objectMapper.writeValueAsString(quotaInfo) + "\n\n");
            writer.flush();

        } catch (Exception e) {
            log.error("Error en streaming IA: {}", e.getMessage(), e);
            try {
                String errorMsg = objectMapper.writeValueAsString(
                        e.getMessage() != null ? e.getMessage() : "Error interno del servidor");
                writer.write("event: error\ndata: {\"message\":" + errorMsg + "}\n\n");
                writer.flush();
            } catch (Exception ignored) {
                // Cliente ya desconectado
            }
        }
    }

    @GetMapping("/quota")
    @Operation(summary = "Consultar cuota actual de consultas IA del usuario")
    public ResponseEntity<AiQuotaResponse> getQuota(@CurrentUser User user) {
        int limit = user.getPlan() == Plan.PREMIUM ? 0 : quotaService.getFreeMonthlyLimit();
        return ResponseEntity.ok(new AiQuotaResponse(
                user.getAiQueriesUsed(),
                limit,
                user.getPlan().name()));
    }
}