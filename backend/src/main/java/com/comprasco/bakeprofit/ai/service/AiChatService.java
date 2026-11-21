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

@Service
public class AiChatService {

    private final CatalogContextLoader catalogLoader;
    private final GeminiClient geminiClient;

    public AiChatService(CatalogContextLoader catalogLoader,
            GeminiClient geminiClient) {
        this.catalogLoader = catalogLoader;
        this.geminiClient = geminiClient;
    }

    public AiChatResponse chat(AiChatRequest request) {
        String catalogText = catalogLoader.loadForSection(request.section());
        String systemPrompt = SectionPrompt.build(request.section(), catalogText);
        List<AiMessage> messages = buildMessages(request);
        String reply = geminiClient.chat(systemPrompt, messages);
        return new AiChatResponse(reply);
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