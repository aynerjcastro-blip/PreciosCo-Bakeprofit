package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se intenta activar (o crear/actualizar como activo) una
 * categoría, producto o tienda cuyo padre/categoría está inactivo.
 * Ej: activar una subcategoría cuya categoría raíz está inactiva,
 * o un producto/tienda cuya categoría está inactiva.
 */
public class InactiveParentException extends RuntimeException {

    public InactiveParentException(String message) {
        super(message);
    }
}