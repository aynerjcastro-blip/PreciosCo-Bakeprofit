package com.comprasco.bakeprofit.exception;

/** Lanzada cuando la opinión no existe o su comentario no es visible (oculto o eliminado) al reportarlo. */
public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(Long id) {
        super("Opinión no encontrada con id: " + id);
    }
}
