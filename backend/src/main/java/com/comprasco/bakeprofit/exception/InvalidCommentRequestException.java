package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando falta un filtro del listado o el reporte no cumple las reglas
 * (OTRO sin detalle, reportar el propio comentario).
 */
public class InvalidCommentRequestException extends RuntimeException {
    public InvalidCommentRequestException(String message) {
        super(message);
    }
}
