package com.comprasco.bakeprofit.ai.dto;

/**
 * Info de cuota del usuario para el frontend.
 *
 * @param queriesUsed  consultas usadas este mes
 * @param queriesLimit límite mensual de consultas (0 = ilimitado / PREMIUM)
 * @param plan         plan del usuario: "FREE" o "PREMIUM"
 * @param recipesUsed  recetas pedidas este mes (SCRUM-325)
 * @param recipesLimit límite mensual de recetas (0 = ilimitado / PREMIUM)
 */
public record AiQuotaResponse(
        int queriesUsed,
        int queriesLimit,
        String plan,
        int recipesUsed,
        int recipesLimit) {
}
