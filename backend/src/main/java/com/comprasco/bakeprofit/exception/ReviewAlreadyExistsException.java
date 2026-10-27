package com.comprasco.bakeprofit.exception;

/** Lanzada cuando el usuario ya opinó sobre ese producto en esa tienda (incluso si borró su comentario). */
public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(String productName, String storeName) {
        super("Ya opinaste sobre el producto '" + productName + "' en la tienda '" + storeName
                + "'. Solo puedes opinar una vez.");
    }
}
