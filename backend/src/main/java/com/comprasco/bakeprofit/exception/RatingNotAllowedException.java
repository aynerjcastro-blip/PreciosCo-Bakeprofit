package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando el producto o la tienda están inactivos y no se puede calificar.
 */
public class RatingNotAllowedException extends RuntimeException {
    public RatingNotAllowedException(String message) {
        super(message);
    }
}
