package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando el comentario no existe o está oculto.
 */
public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(Long id) {
        super("Comentario no encontrado con id: " + id);
    }
}
