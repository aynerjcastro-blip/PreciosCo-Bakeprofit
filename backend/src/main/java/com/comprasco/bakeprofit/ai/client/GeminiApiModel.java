package com.comprasco.bakeprofit.ai.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Estructura interna que representa los objetos JSON de la API de Gemini.
 * No se exponen al frontend — solo las usa GeminiClient.
 *
 * La API espera: { "system_instruction": {...}, "contents": [...],
 * "generationConfig": {...} }
 */
class GeminiApiModel {

    /** Una parte de texto: { "text": "..." } */
    record Part(String text) {
    }

    /** Un mensaje en la conversación: { "role": "user", "parts": [...] } */
    record Content(String role, List<Part> parts) {
    }

    /** Instrucción del sistema: { "parts": [{ "text": "..." }] } — sin role */
    record SystemInstruction(List<Part> parts) {

        static SystemInstruction of(String text) {
            return new SystemInstruction(List.of(new Part(text)));
        }
    }

    /**
     * Configuración de razonamiento. Los modelos Gemini 3 usan
     * {@code thinkingLevel}; NO aceptan {@code thinkingBudget: 0}
     * (responden 400 INVALID_ARGUMENT).
     */
    record ThinkingConfig(String thinkingLevel) {
    }

    /**
     * Configuración de generación. {@code responseMimeType} +
     * {@code responseSchema} fuerzan una respuesta JSON con esa forma
     * (salida estructurada).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(ThinkingConfig thinkingConfig, String responseMimeType, Object responseSchema) {
    }

    /** El body completo del POST a generateContent / streamGenerateContent */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerateRequest(
            SystemInstruction system_instruction,
            List<Content> contents,
            GenerationConfig generationConfig) {
    }

    private GeminiApiModel() {
    } // No instanciable
}
