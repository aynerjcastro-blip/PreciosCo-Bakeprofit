package com.comprasco.bakeprofit.ai.dto;

import java.util.List;

/**
 * Representa un mensaje en la conversación con la IA.
 * Es el tipo público que cruza entre paquetes — el servicio lo usa,
 * y GeminiClient lo convierte internamente al formato de Gemini.
 */
public record AiMessage(String role, List<String> parts) {

    public static AiMessage of(String role, String text) {
        return new AiMessage(role, List.of(text));
    }
}