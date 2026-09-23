package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se intenta crear una tienda con un nombre 
 * que ya está registrado.
 */
public class StoreAlreadyExistsException extends RuntimeException {
    
    public StoreAlreadyExistsException (String name) {
        super("Ya existe una tienda registrada con el nombre: " + name);
    }
}
