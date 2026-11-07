package com.comprasco.bakeprofit.entity;

/**
 * Estado de una oferta del staging del scraper.
 * PENDING: recién cargada. PROCESSED: el procesador la convirtió en precio.
 * UNCLASSIFIED: no pasó los filtros; {@code reason} explica por qué.
 */
public enum OfferStatus {
    PENDING, PROCESSED, UNCLASSIFIED;

    /** Longitud de la columna VARCHAR que guarda el valor en la BD. */
    public static final int COLUMN_LENGTH = 15;
}