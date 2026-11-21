package com.comprasco.bakeprofit.ai.client;

import com.comprasco.bakeprofit.ai.exception.AiEmptyResponseException;

/**
 * Estructura interna que representa los objetos JSON de la API de Gemini.
 * No se exponen al frontend — solo las usa GeminiClient.
 *
 * La API espera: { "system_instruction": {...}, "contents": [...] }
 * Y responde: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }]
 * }
 */
class GeminiApiModel {

    // --- Request ---

    /** Una parte de texto: { "text": "..." } */
    record Part(String text) {
    }

    /** Un mensaje en la conversación: { "role": "user", "parts": [...] } */
    record Content(String role, java.util.List<Part> parts) {

        static Content of(String role, String text) {
            return new Content(role, java.util.List.of(new Part(text)));
        }
    }

    /** Instrucción del sistema: { "parts": [{ "text": "..." }] } — sin role */
    record SystemInstruction(java.util.List<Part> parts) {

        static SystemInstruction of(String text) {
            return new SystemInstruction(java.util.List.of(new Part(text)));
        }
    }

    /** El body completo del POST a generateContent */
    record GenerateRequest(
            SystemInstruction system_instruction,
            java.util.List<Content> contents) {
    }

    // --- Response ---

    /** Un candidato de respuesta */
    record Candidate(Content content, String finishReason) {
    }

    /** La respuesta completa de Gemini */
    record GenerateResponse(java.util.List<Candidate> candidates) {

        /** Extrae el texto del primer candidato, o lanza si no hay respuesta */
        String text() {
            if (candidates == null || candidates.isEmpty()) {
                throw new AiEmptyResponseException();
            }
            var parts = candidates.getFirst().content().parts();
            if (parts == null || parts.isEmpty()) {
                throw new AiEmptyResponseException();
            }
            return parts.getFirst().text();
        }
    }

    private GeminiApiModel() {
    } // No instanciable
}