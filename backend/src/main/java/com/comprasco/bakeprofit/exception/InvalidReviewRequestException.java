package com.comprasco.bakeprofit.exception;

/** Lanzada cuando falta un filtro del listado o el reporte no cumple las reglas. */
public class InvalidReviewRequestException extends RuntimeException {
    public InvalidReviewRequestException(String message) {
        super(message);
    }
}
