package com.comprasco.bakeprofit.auth.exception;

/**
 * Lanzada cuando un cambio de contraseña no es válido: la contraseña actual no
 * coincide o la nueva es igual a la actual (se responde 400).
 * <p>
 * Se usa 400 y no 401 a propósito: un 401 hace que el frontend crea que la
 * sesión venció.
 */
public class InvalidPasswordChangeException extends RuntimeException {
    public InvalidPasswordChangeException(String message) {
        super(message);
    }
}