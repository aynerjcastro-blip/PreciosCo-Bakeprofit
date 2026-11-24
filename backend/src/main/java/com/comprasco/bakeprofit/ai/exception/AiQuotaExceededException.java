package com.comprasco.bakeprofit.ai.exception;

/**
 * Se lanza cuando el usuario FREE agotó su cuota mensual de consultas IA.
 */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(int used, int limit) {
        super("Has alcanzado el límite de %d consultas de IA este mes (usadas: %d)"
                .formatted(limit, used));
    }
}