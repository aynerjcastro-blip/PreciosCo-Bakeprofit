package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se intenta buscar por id 
 * una tienda que no existe.
 */
public class StoreNotFoundException extends RuntimeException {

    public StoreNotFoundException (String message) {
        super(message);
    }
}
