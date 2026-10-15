package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando no se puede comentar (producto o tienda inactivos) o el reporte ya existe (carrera).
 */
public class CommentNotAllowedException extends RuntimeException {
    public CommentNotAllowedException(String message) {
        super(message);
    }
}
