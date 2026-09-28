package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando una operación viola la jerarquía de categorías
 * (raíz → subcategoría): 
 * - Autorreferencia
 * - Padre que no es raíz
 * - Asignar padre a una categoría con subcategorías
 * - Asignar a una tienda una categoría que no es raíz
 * - Asignar padre a una categoría con tiendas en su dominio
 * - Asignar padre como categoría a un producto
 */
public class InvalidCategoryHierarchyException extends RuntimeException {

    public InvalidCategoryHierarchyException (String message) {
        super(message);
    }
}
