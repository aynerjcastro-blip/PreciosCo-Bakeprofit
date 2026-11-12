package com.comprasco.bakeprofit.catalog.exception;

/**
 * Lanzada cuando los datos de identidad de un producto no son coherentes entre sí:
 * contenido neto sin unidad base (o al revés), o referencia del fabricante sin marca.
 * El handler la convierte en HTTP 400 BAD REQUEST.
 */
public class InvalidProductIdentityException extends RuntimeException {

    public InvalidProductIdentityException(String message) {
        super(message);
    }
}