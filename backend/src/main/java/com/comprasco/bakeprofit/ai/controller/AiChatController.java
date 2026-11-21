package com.comprasco.bakeprofit.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.ai.dto.AiChatRequest;
import com.comprasco.bakeprofit.ai.dto.AiChatResponse;
import com.comprasco.bakeprofit.ai.service.AiChatService;
import com.comprasco.bakeprofit.auth.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.comprasco.bakeprofit.auth.entity.User;

import jakarta.validation.Valid;

@Tag(name="Integracion de IA")
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @CurrentUser User user,
            @Valid @RequestBody AiChatRequest request) {

        AiChatResponse response = aiChatService.chat(request);
        return ResponseEntity.ok(response);
    }
}