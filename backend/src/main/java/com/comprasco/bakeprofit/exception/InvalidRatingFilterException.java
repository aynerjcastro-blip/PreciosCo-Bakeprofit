package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando el resumen de calificaciones se pide sin productId ni storeId.
 */
public class InvalidRatingFilterException extends RuntimeException {
    public InvalidRatingFilterException(String message) {
        super(message);
    }
}
