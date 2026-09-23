package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando se intenta registrar un email que ya existe en la base de datos.
 * El handler la convierte en HTTP 409 CONFLICT.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("El email ya esta registrado: " + email);
    }
}