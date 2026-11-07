package com.comprasco.bakeprofit.util;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Única fuente de verdad sobre códigos GTIN de GS1 (EAN-8, UPC-A, EAN-13 o GTIN-14).
 *
 * <p>
 * El catálogo guarda siempre el GTIN-14: el código rellenado con ceros a la
 * izquierda ({@link #normalize(String)}). Así un UPC-A de 12 dígitos y su EAN-13
 * equivalente son el mismo producto y chocan en el índice único
 * {@code uq_products_ean}. Rellenar con ceros no altera el dígito verificador,
 * porque los pesos se cuentan desde la derecha.
 * </p>
 *
 * <p>
 * Qué tipo de código hay dentro de un GTIN-14 lo decide solo {@link #classify(String)};
 * {@link #assess(String)} y {@link #toDisplay(String)} se apoyan en esa clasificación,
 * de modo que la longitud con la que llegó el código no cambia el resultado.
 * </p>
 */
public final class Gtin {

    public static final int LENGTH = 14;

    private static final Set<Integer> ACCEPTED_LENGTHS = Set.of(8, 12, 13, 14);
    private static final Pattern DIGITS_ONLY = Pattern.compile("[0-9]+");
    private static final Pattern ALL_ZEROS = Pattern.compile("0+");
    private static final String PADDING_DIGIT = "0";
    private static final int CHECK_DIGIT_MODULUS = 10;
    // En el GTIN-14, las posiciones impares (desde la izquierda) pesan 3 y las pares pesan 1
    private static final int ODD_POSITION_WEIGHT = 3;
    private static final int EVEN_POSITION_WEIGHT = 1;

    // Regla de GS1 para campos de 14 dígitos
    private static final String GTIN_8_MARKER = "000000";
    private static final char GTIN_13_INDICATOR = '0';
    private static final char VARIABLE_MEASURE_INDICATOR = '9';
    private static final int GTIN_8_LENGTH = 8;
    private static final int GTIN_13_LENGTH = 13;

    // Prefijos GS1 de circulación restringida: los asigna cada comercio, no el fabricante
    private static final List<String> RESTRICTED_GTIN_8_PREFIXES = List.of("0", "2");
    private static final List<String> RESTRICTED_GTIN_13_PREFIXES = List.of("02", "04", "2");

    private static final String INVALID_FORMAT_MESSAGE =
            "El GTIN debe tener 8, 12, 13 o 14 dígitos: %s";
    private static final String NOT_GTIN_14_MESSAGE =
            "Se esperaba un GTIN-14 de exactamente 14 dígitos: %s";

    private Gtin() {
    }

    /**
     * Evalúa si un código, tal como llegó, sirve como identidad de producto.
     *
     * <p>
     * El orden importa: INVALID, luego PLACEHOLDER y por último la clasificación.
     * "00000000000000" tiene un dígito verificador correcto y se clasificaría como un
     * EAN-8 con prefijo 0 (STORE_INTERNAL); revisar los ceros antes hace que se
     * reporte como lo que es, un valor de relleno.
     * </p>
     */
    public static GtinVerdict assess(String raw) {
        if (!hasAcceptedFormat(raw) || !hasValidCheckDigit(pad(raw))) {
            return GtinVerdict.INVALID;
        }
        if (ALL_ZEROS.matcher(raw).matches()) {
            return GtinVerdict.PLACEHOLDER;
        }
        String gtin14 = pad(raw);
        return switch (classify(gtin14)) {
            case GTIN_8 -> verdictByPrefix(lastDigits(gtin14, GTIN_8_LENGTH), RESTRICTED_GTIN_8_PREFIXES);
            case GTIN_13 -> verdictByPrefix(lastDigits(gtin14, GTIN_13_LENGTH), RESTRICTED_GTIN_13_PREFIXES);
            case VARIABLE_MEASURE -> GtinVerdict.VARIABLE_MEASURE;
            case PACKAGING -> GtinVerdict.COMPARABLE;
        };
    }

    /** "036000291452" (UPC-A) y "0036000291452" (EAN-13) dan "00036000291452". Nulo da nulo. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        if (!hasAcceptedFormat(raw)) {
            throw new IllegalArgumentException(INVALID_FORMAT_MESSAGE.formatted(raw));
        }
        return pad(raw);
    }

    /**
     * Forma para mostrar un GTIN-14 guardado: el código que aparece en el empaque.
     * "00000096385074" se muestra como "96385074" y "07702552000141" como
     * "7702552000141"; los empaques se muestran con sus 14 dígitos.
     */
    public static String toDisplay(String gtin14) {
        if (gtin14 == null) {
            return null;
        }
        return switch (classify(gtin14)) {
            case GTIN_8 -> lastDigits(gtin14, GTIN_8_LENGTH);
            case GTIN_13 -> lastDigits(gtin14, GTIN_13_LENGTH);
            case VARIABLE_MEASURE, PACKAGING -> gtin14;
        };
    }

    /**
     * Regla de GS1 para un campo de 14 dígitos, evaluada en este orden: seis ceros
     * iniciales → GTIN-8; empieza por 0 → GTIN-13; empieza por 9 → medida variable;
     * cualquier otro dígito → empaque.
     *
     * @throws IllegalArgumentException si no recibe exactamente 14 dígitos
     */
    static GtinKind classify(String gtin14) {
        if (gtin14 == null || gtin14.length() != LENGTH || !DIGITS_ONLY.matcher(gtin14).matches()) {
            throw new IllegalArgumentException(NOT_GTIN_14_MESSAGE.formatted(gtin14));
        }
        if (gtin14.startsWith(GTIN_8_MARKER)) {
            return GtinKind.GTIN_8;
        }
        char indicator = gtin14.charAt(0);
        if (indicator == GTIN_13_INDICATOR) {
            return GtinKind.GTIN_13;
        }
        if (indicator == VARIABLE_MEASURE_INDICATOR) {
            return GtinKind.VARIABLE_MEASURE;
        }
        return GtinKind.PACKAGING;
    }

    private static GtinVerdict verdictByPrefix(String code, List<String> restrictedPrefixes) {
        boolean restricted = restrictedPrefixes.stream().anyMatch(code::startsWith);
        return restricted ? GtinVerdict.STORE_INTERNAL : GtinVerdict.COMPARABLE;
    }

    private static boolean hasAcceptedFormat(String raw) {
        return raw != null
                && ACCEPTED_LENGTHS.contains(raw.length())
                && DIGITS_ONLY.matcher(raw).matches();
    }

    private static boolean hasValidCheckDigit(String gtin14) {
        int lastIndex = LENGTH - 1;
        return checkDigitOf(gtin14.substring(0, lastIndex)) == digitAt(gtin14, lastIndex);
    }

    private static int checkDigitOf(String first13Digits) {
        int sum = 0;
        for (int index = 0; index < first13Digits.length(); index++) {
            int weight = index % 2 == 0 ? ODD_POSITION_WEIGHT : EVEN_POSITION_WEIGHT;
            sum += digitAt(first13Digits, index) * weight;
        }
        return (CHECK_DIGIT_MODULUS - sum % CHECK_DIGIT_MODULUS) % CHECK_DIGIT_MODULUS;
    }

    private static int digitAt(String digits, int index) {
        return Character.digit(digits.charAt(index), CHECK_DIGIT_MODULUS);
    }

    private static String pad(String raw) {
        return PADDING_DIGIT.repeat(LENGTH - raw.length()) + raw;
    }

    private static String lastDigits(String gtin14, int count) {
        return gtin14.substring(LENGTH - count);
    }
}