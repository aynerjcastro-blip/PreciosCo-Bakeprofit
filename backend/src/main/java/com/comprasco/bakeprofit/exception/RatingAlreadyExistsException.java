package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando el usuario ya calificó ese producto en esa tienda.
 */
public class RatingAlreadyExistsException extends RuntimeException {
    public RatingAlreadyExistsException(String productName, String storeName) {
        super("Ya calificaste el producto '" + productName + "' en la tienda '" + storeName + "'.");
    }
}
