package com.comprasco.bakeprofit.follow.exception;

/** Lanzada cuando el usuario ya sigue ese producto (se responde 409). */
public class FollowAlreadyExistsException extends RuntimeException {
    public FollowAlreadyExistsException(String productName) {
        super("Ya sigues el producto '" + productName + "'.");
    }
}