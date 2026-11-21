package com.comprasco.bakeprofit.ai.exception;

/**
 * La API de IA no devolvió texto utilizable.
 */
public class AiEmptyResponseException extends RuntimeException {

    public AiEmptyResponseException() {
        super("La IA no generó una respuesta válida");
    }
}