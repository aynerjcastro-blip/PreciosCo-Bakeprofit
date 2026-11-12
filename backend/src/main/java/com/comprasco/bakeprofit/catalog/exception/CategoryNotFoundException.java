package com.comprasco.bakeprofit.catalog.exception;

/**
 * lanzada cuando se intenta buscar por medio del id 
 * una categoria que no existe.
 */
public class CategoryNotFoundException extends RuntimeException {
    
    public CategoryNotFoundException (String message) {
        super(message);
    }
}
