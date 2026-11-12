package com.comprasco.bakeprofit.core.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Prepara el texto que escribe el usuario para buscar por nombre.
 *
 * <p>
 * Solo parte el texto en palabras y escapa los comodines de LIKE. Las tildes y las
 * mayúsculas las quita PostgreSQL con {@code lower(f_unaccent(...))}, a los dos lados de la
 * comparación, para que la regla viva en un solo lugar
 * (scripts/2026-11-scrum140-busqueda-sin-tildes.sql).
 * </p>
 */
public final class SearchTerms {

    /** Carácter de escape que se declara en el LIKE. */
    public static final char LIKE_ESCAPE = '\\';

    private static final String WHITESPACE = "\\s+";
    private static final String ANY_TEXT = "%";

    private SearchTerms() {
    }

    /** Palabras del texto, sin espacios sobrantes; una lista vacía si no hay texto. */
    public static List<String> words(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.asList(text.strip().split(WHITESPACE));
    }

    /**
     * Patrón LIKE "contiene esta palabra". Un "%" o un "_" escritos por el usuario se
     * buscan como texto y no como comodines: "50%" no debe encontrar "500 g".
     */
    public static String containsPattern(String word) {
        return ANY_TEXT + escapeLikeWildcards(word) + ANY_TEXT;
    }

    private static String escapeLikeWildcards(String word) {
        String escape = String.valueOf(LIKE_ESCAPE);
        return word.replace(escape, escape + escape)
                .replace("%", escape + "%")
                .replace("_", escape + "_");
    }
}
