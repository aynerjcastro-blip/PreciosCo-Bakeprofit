package com.comprasco.bakeprofit.exception.response;

import java.time.LocalDateTime;

/**
 * Body estándar para todos los errores HTTP que devuelve la API.
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    LocalDateTime timestamp
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now());
    }
}