package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando una operación viola la jerarquía de categorías
 * (raíz → subcategoría): autorreferencia, padre que no es raíz,
 * asignar padre a una categoría con subcategorías, o asignar
 * a una tienda una categoría que no es raíz.
 */
public class InvalidCategoryHierarchyException extends RuntimeException {

    public InvalidCategoryHierarchyException (String message) {
        super(message);
    }
}
