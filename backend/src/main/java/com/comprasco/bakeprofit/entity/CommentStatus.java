package com.comprasco.bakeprofit.entity;

/**
 * Estado de visibilidad de un comentario.
 * PUBLICADO: visible. REPORTADO: tiene reportes pendientes (sigue visible).
 * CONSERVADO: un moderador lo revisó y lo mantuvo. OCULTO: no aparece en ningún listado.
 * ELIMINADO: no aparece en listado y fue ocultado por decisión del usuario.
 * En la Fase 2 solo la API pasa de PUBLICADO/CONSERVADO a REPORTADO; el resto se cambia a mano en la BD.
 */
public enum CommentStatus {
    PUBLICADO, REPORTADO, CONSERVADO, OCULTO, ELIMINADO
}
