package com.comprasco.bakeprofit.catalog.exception;

/** Lanzada cuando un filtro de la búsqueda de productos no es válido (400). */
public class InvalidSearchFilterException extends RuntimeException {

    public InvalidSearchFilterException(String message) {
        super(message);
    }
}
