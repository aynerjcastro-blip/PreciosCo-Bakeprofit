package com.comprasco.bakeprofit.entity;

/**
 * Unidad base en la que se expresa el contenido neto de un producto:
 * gramos (G), mililitros (ML) o unidades (UN). Los tamaños se comparan
 * como número en esta unidad, nunca como texto.
 */
public enum UnitBase {
    G, ML, UN;

    /** Longitud de la columna VARCHAR que guarda el valor en la BD. */
    public static final int COLUMN_LENGTH = 2;
}