package com.comprasco.bakeprofit.follow.exception;

/** Lanzada cuando no se puede seguir un producto, por ejemplo porque está inactivo (se responde 409). */
public class FollowNotAllowedException extends RuntimeException {
    public FollowNotAllowedException(String message) {
        super(message);
    }
}