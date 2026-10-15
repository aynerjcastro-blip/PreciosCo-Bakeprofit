package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando el rol del usuario no puede realizar la acción (se responde 403).
 */
public class ForbiddenRoleException extends RuntimeException {
    public ForbiddenRoleException(String message) {
        super(message);
    }
}
