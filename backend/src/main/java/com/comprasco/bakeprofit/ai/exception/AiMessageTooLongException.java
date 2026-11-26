package com.comprasco.bakeprofit.ai.exception;

/**
 * Se lanza cuando el mensaje del usuario FREE supera el límite de caracteres
 * permitido para la cotización (RF60).
 */
public class AiMessageTooLongException extends RuntimeException {

    public AiMessageTooLongException(int length, int limit) {
        super("El mensaje tiene %d caracteres y el límite gratuito es de %d"
                .formatted(length, limit));
    }
}