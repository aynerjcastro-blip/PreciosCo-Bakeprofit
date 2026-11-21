package com.comprasco.bakeprofit.ai.dto;

/**
 * Respuesta del chat de IA al frontend.
 *
 * @param reply texto generado por el asistente
 */
public record AiChatResponse(String reply) {
}