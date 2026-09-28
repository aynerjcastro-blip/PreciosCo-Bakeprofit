package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se intenta asignar una categoría padre inválida:
 * la categoría se referencia a sí misma, o el padre indicado
 * no es una categoría raíz.
 */
public class InvalidCategoryParentException extends RuntimeException {

    public InvalidCategoryParentException (String message) {
        super(message);
    }
}