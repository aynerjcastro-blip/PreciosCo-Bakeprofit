package com.comprasco.bakeprofit.ai.dto;

/**
 * Respuesta del chat de IA al frontend.
 *
 * @param reply        texto generado por el asistente
 * @param queriesUsed  consultas usadas este mes (después de esta consulta)
 * @param queriesLimit límite mensual (0 = ilimitado / PREMIUM)
 * @param plan         plan del usuario: "FREE" o "PREMIUM"
 */
public record AiChatResponse(
        String reply,
        int queriesUsed,
        int queriesLimit,
        String plan) {
}