package com.comprasco.bakeprofit.exception;

/** Lanzada cuando la acción no es posible por el estado del recurso (inactivo, plazo vencido, comentario bloqueado, reporte duplicado). */
public class ReviewNotAllowedException extends RuntimeException {
    public ReviewNotAllowedException(String message) {
        super(message);
    }
}
