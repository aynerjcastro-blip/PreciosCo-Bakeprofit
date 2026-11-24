package com.comprasco.bakeprofit.ai.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.context.CatalogContextLoader;
import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.prompt.SectionPrompt;
import com.comprasco.bakeprofit.auth.entity.Plan;
import com.comprasco.bakeprofit.auth.entity.User;

/**
 * Orquesta la consulta de chat IA:
 * 1. Valida cuota del usuario
 * 2. Construye el prompt con contexto del catálogo
 * 3. Llama a Gemini (normal o streaming)
 * 4. Incrementa el contador de consultas
 * 5. Retorna la respuesta con info de cuota
 */
@Service
public class AiChatService {

    private final CatalogContextLoader catalogLoader;
    private final GeminiClient geminiClient;
    private final AiQuotaService quotaService;

    public AiChatService(CatalogContextLoader catalogLoader,
            GeminiClient geminiClient,
            AiQuotaService quotaService) {
        this.catalogLoader = catalogLoader;
        this.geminiClient = geminiClient;
        this.quotaService = quotaService;
    }

    /**
     * Procesa una consulta de chat IA sin streaming (fallback).
     */
    public AiChatResponse chat(AiChatRequest request, User user) {
        quotaService.assertQuotaAvailable(user);

        String catalogText = catalogLoader.loadForSection(request.section());
        String systemPrompt = SectionPrompt.build(request.section(), catalogText);
        List<AiMessage> messages = buildMessages(request);
        String reply = geminiClient.chat(systemPrompt, messages);

        quotaService.incrementUsage(user);

        int used = user.getAiQueriesUsed() + 1;
        int limit = user.getPlan() == Plan.PREMIUM ? 0 : quotaService.getFreeMonthlyLimit();

        return new AiChatResponse(reply, used, limit, user.getPlan().name());
    }

    /**
     * Procesa una consulta de chat IA con streaming.
     * Cada fragmento de texto se emite vía el callback {@code onChunk}.
     * Al finalizar, incrementa la cuota y retorna la info de cuota.
     *
     * @param request datos del mensaje
     * @param user    usuario autenticado
     * @param onChunk callback para cada fragmento de texto
     * @return info de cuota post-consulta
     */
    public AiChatResponse chatStream(AiChatRequest request, User user,
            Consumer<String> onChunk)
            throws IOException, InterruptedException {

        quotaService.assertQuotaAvailable(user);

        String catalogText = catalogLoader.loadForSection(request.section());
        String systemPrompt = SectionPrompt.build(request.section(), catalogText);
        List<AiMessage> messages = buildMessages(request);

        geminiClient.chatStream(systemPrompt, messages, onChunk);

        quotaService.incrementUsage(user);

        int used = user.getAiQueriesUsed() + 1;
        int limit = user.getPlan() == Plan.PREMIUM ? 0 : quotaService.getFreeMonthlyLimit();

        return new AiChatResponse("", used, limit, user.getPlan().name());
    }

    private List<AiMessage> buildMessages(AiChatRequest request) {
        List<AiMessage> messages = new ArrayList<>();

        if (request.history() != null) {
            for (AiChatRequest.Turn turn : request.history()) {
                String geminiRole = "assistant".equals(turn.role()) ? "model" : turn.role();
                messages.add(AiMessage.of(geminiRole, turn.content()));
            }
        }

        messages.add(AiMessage.of("user", request.message()));
        return messages;
    }
}