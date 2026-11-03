package com.comprasco.bakeprofit.exception;

/** Lanzada cuando el rol o el usuario no pueden realizar la acción (se responde 403). */
public class ForbiddenActionException extends RuntimeException {
    public ForbiddenActionException(String message) {
        super(message);
    }
}
