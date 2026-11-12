package com.comprasco.bakeprofit.catalog.exception;

/**
 * Lanzada cuando ya existe un producto con el mismo EAN o con la misma
 * marca y referencia del fabricante. El handler la convierte en HTTP 409 CONFLICT.
 */
public class ProductAlreadyExistsException extends RuntimeException {

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}