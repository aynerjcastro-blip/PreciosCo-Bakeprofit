package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se intenta buscar por id 
 * un producto que no existe.
 */
public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException (String message) {
        super(message);
    }
}
