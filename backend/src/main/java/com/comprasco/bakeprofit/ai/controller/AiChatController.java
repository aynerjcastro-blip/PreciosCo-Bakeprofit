package com.comprasco.bakeprofit.ai.controller;

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
import com.comprasco.bakeprofit.auth.security.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.comprasco.bakeprofit.auth.entity.User;

import jakarta.validation.Valid;

@Tag(name = "Integracion de IA")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiQuotaService quotaService;

    public AiChatController(AiChatService aiChatService, AiQuotaService quotaService) {
        this.aiChatService = aiChatService;
        this.quotaService = quotaService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Enviar mensaje al chat IA con control de cuota")
    public ResponseEntity<AiChatResponse> chat(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request) {

        AiChatResponse response = aiChatService.chat(request, user);
        return ResponseEntity.ok(response);
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