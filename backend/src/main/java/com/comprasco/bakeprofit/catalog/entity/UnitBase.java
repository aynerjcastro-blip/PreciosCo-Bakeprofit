package com.comprasco.bakeprofit.catalog.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Unidad base en la que se expresa el contenido neto de un producto:
 * gramos (G), mililitros (ML) o unidades (UN). Los tamaños se comparan
 * como número en esta unidad, nunca como texto.
 */
public enum UnitBase {
    G, ML, UN;

    /** Longitud de la columna VARCHAR que guarda el valor en la BD. */
    public static final int COLUMN_LENGTH = 2;

    /**
     * Interpreta la unidad cruda que escribe el scraper. Vacío si es nula o desconocida:
     * el texto viene sin validar y no debe lanzar excepción.
     */
    public static Optional<UnitBase> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String code = raw.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(unit -> unit.name().equals(code))
                .findFirst();
    }
}