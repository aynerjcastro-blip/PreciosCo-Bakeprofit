package com.comprasco.bakeprofit.exception;

/** Lanzada cuando el seguimiento no existe o no pertenece al usuario (se responde 404). */
public class FollowNotFoundException extends RuntimeException {
    public FollowNotFoundException(Long id) {
        super("No se encontró el seguimiento con id " + id + ".");
    }
}