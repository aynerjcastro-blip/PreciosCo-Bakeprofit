package com.comprasco.bakeprofit.ai.exception;

public class AiEmptyResponseException extends RuntimeException {

    public AiEmptyResponseException() {
        super("La API de IA no devolvió texto utilizable");
    }

    public AiEmptyResponseException(String message) {
        super(message);
    }
}