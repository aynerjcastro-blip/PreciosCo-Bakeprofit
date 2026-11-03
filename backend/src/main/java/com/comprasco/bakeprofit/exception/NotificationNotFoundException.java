package com.comprasco.bakeprofit.exception;

/** Lanzada cuando el aviso no existe o no pertenece al usuario (se responde 404). */
public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("No se encontró el aviso con id " + id + ".");
    }
}