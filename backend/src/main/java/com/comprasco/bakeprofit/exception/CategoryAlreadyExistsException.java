package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se quiere registrar una nueva categoría con un nombre
 * ya existente dentro del sistema.
 */
public class CategoryAlreadyExistsException extends RuntimeException {
    
    public CategoryAlreadyExistsException (String categoria) {
        super("Esta categoria ya ha sido registrada en el sistema: " + categoria);
    }
}