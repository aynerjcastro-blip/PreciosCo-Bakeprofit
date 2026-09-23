package com.comprasco.bakeprofit.exception;

/**
 * Para login. Usala cuando quieras devolver siempre el mismo mensaje
 * sin filtrar si falló el email o la password.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciales invalidas");
    }
}