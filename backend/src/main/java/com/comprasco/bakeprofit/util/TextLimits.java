package com.comprasco.bakeprofit.util;

/**
 * Ajusta textos externos a la longitud de su columna antes de guardarlos.
 *
 * <p>
 * Un nombre largo de una tienda no es un problema de calidad de la oferta: si el INSERT
 * fallara por longitud, la oferta quedaría sin clasificar por un límite del esquema.
 * </p>
 */
public final class TextLimits {

    private TextLimits() {
    }

    /**
     * Recorta a {@code maxLength} puntos de código: VARCHAR(n) en PostgreSQL cuenta
     * caracteres, no unidades UTF-16, y así no se parte un emoji ni una tilde compuesta
     * en dos mitades inválidas.
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.codePointCount(0, text.length()) <= maxLength) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, maxLength));
    }
}
