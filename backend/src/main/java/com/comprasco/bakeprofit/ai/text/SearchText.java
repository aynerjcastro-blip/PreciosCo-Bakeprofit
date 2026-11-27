package com.comprasco.bakeprofit.ai.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalización de texto para comparar nombres de producto con términos:
 * minúsculas, sin tildes y con cualquier separador convertido en un espacio.
 */
public final class SearchText {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SearchText() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String noAccents = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        return NON_ALNUM.matcher(noAccents.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }

    /** ¿El texto (ya normalizado y rodeado de espacios) contiene el término como palabras completas? */
    public static boolean containsWords(String paddedNormalizedText, String normalizedTerm) {
        return paddedNormalizedText.contains(" " + normalizedTerm + " ");
    }
}
