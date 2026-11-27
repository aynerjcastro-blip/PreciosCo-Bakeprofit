package com.comprasco.bakeprofit.ai.exception;

/**
 * Se lanza cuando el usuario FREE agotó su cuota mensual de recetas (SCRUM-325).
 */
public class AiRecipeQuotaExceededException extends RuntimeException {

    public AiRecipeQuotaExceededException(int limit) {
        super("Has alcanzado el límite de %d recetas este mes. Actualiza a Premium para recetas ilimitadas."
                .formatted(limit));
    }
}
