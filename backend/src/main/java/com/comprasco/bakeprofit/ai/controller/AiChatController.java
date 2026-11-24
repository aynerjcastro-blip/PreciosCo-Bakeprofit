package com.comprasco.bakeprofit.ai.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
     * Eventos emitidos:
     * </p>
     * <ul>
     * <li>{@code event: chunk} → fragmento de texto: {@code {"text":"..."}}</li>
     * <li>{@code event: done} → fin del stream con info de cuota:
     * {@code {"queriesUsed":1,"queriesLimit":20,"plan":"FREE"}}</li>
     * <li>{@code event: error} → error durante el stream:
     * {@code {"message":"..."}}</li>
     * </ul>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Enviar mensaje al chat IA con streaming SSE")
    public SseEmitter chatStream(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request) {

        // Timeout de 2 minutos (Gemini puede tardar en modelos pesados)
        SseEmitter emitter = new SseEmitter(120_000L);

        // Ejecutar en hilo aparte para no bloquear el thread del servlet
        Thread.startVirtualThread(() -> {
            try {
                AiChatResponse quotaInfo = aiChatService.chatStream(
                        request,
                        user,
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data("{\"text\":" + objectMapper.writeValueAsString(chunk) + "}"));
                            } catch (IOException e) {
                                // Cliente desconectado — no es un error grave
                                log.debug("Cliente desconectado durante streaming: {}", e.getMessage());
                            }
                        });

                // Enviar evento final con la info de cuota
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(objectMapper.writeValueAsString(quotaInfo)));

                emitter.complete();

            } catch (Exception e) {
                log.error("Error en streaming IA: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"message\":" + objectMapper.writeValueAsString(e.getMessage()) + "}"));
                } catch (IOException ignored) {
                    // Cliente ya desconectado
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
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