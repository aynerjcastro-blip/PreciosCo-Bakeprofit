package com.comprasco.bakeprofit.ai.exception;

/**
 * Petición de chat IA inválida (sección desconocida, historial mal formado...).
 * Se traduce a HTTP 400.
 */
public class AiInvalidRequestException extends RuntimeException {

    public AiInvalidRequestException(String message) {
        super(message);
    }
}
