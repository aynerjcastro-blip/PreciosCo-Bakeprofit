package com.comprasco.bakeprofit.ai.service;

import java.util.ArrayList;
import java.util.List;

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
 * 3. Llama a Gemini
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
     * Procesa una consulta de chat IA con control de cuota.
     *
     * @param request datos del mensaje del usuario
     * @param user    usuario autenticado (para cuota)
     * @return respuesta con texto de la IA + info de cuota
     */
    public AiChatResponse chat(AiChatRequest request, User user) {
        // 1. Validar cuota antes de llamar a la IA
        quotaService.assertQuotaAvailable(user);

        // 2. Construir prompt y llamar a Gemini
        String catalogText = catalogLoader.loadForSection(request.section());
        String systemPrompt = SectionPrompt.build(request.section(), catalogText);
        List<AiMessage> messages = buildMessages(request);
        String reply = geminiClient.chat(systemPrompt, messages);

        // 3. Incrementar contador (solo si la respuesta fue exitosa)
        quotaService.incrementUsage(user);

        // 4. Calcular cuota actual para la respuesta
        int used = user.getAiQueriesUsed() + 1; // +1 porque el increment es async en BD
        int limit = user.getPlan() == Plan.PREMIUM ? 0 : quotaService.getFreeMonthlyLimit();

        return new AiChatResponse(reply, used, limit, user.getPlan().name());
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