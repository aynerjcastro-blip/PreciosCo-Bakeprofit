package com.comprasco.bakeprofit.ai.dto;

/**
 * Info de cuota del usuario para el frontend.
 *
 * @param queriesUsed  consultas usadas este mes
 * @param queriesLimit límite mensual (0 = ilimitado / PREMIUM)
 * @param plan         plan del usuario: "FREE" o "PREMIUM"
 */
public record AiQuotaResponse(
        int queriesUsed,
        int queriesLimit,
        String plan) {
}