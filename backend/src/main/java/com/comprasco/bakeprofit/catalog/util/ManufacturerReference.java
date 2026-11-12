package com.comprasco.bakeprofit.catalog.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Regla única de normalización de la referencia del fabricante (repuestos, ADR-01):
 * solo letras y números ASCII, en mayúsculas.
 *
 * <p>
 * Debe dar exactamente el mismo resultado que la columna generada
 * {@code products.manufacturer_ref_norm} del script SQL:
 * {@code upper(regexp_replace(manufacturer_ref, '[^A-Za-z0-9]', '', 'g'))}.
 * Java la usa para comprobar duplicados antes de guardar; la BD la usa en el
 * índice único {@code uq_products_brand_ref}. Si cambia una, debe cambiar la otra
 * en el mismo commit, o la comprobación previa y el índice dejarán de coincidir.
 * </p>
 */
public final class ManufacturerReference {

    // Todo lo que no sea letra o número ASCII es separador: espacios, guiones, puntos, barras.
    // Las letras con tilde y la Ñ también se eliminan, igual que en la expresión de PostgreSQL.
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");
    private static final String REMOVED = "";

    private ManufacturerReference() {
    }

    /** "0 451 103 318" da "0451103318"; "A 000/420" da "A000420". Una referencia nula da nulo. */
    public static String normalize(String reference) {
        if (reference == null) {
            return null;
        }
        return NOT_ALPHANUMERIC.matcher(reference)
                .replaceAll(REMOVED)
                .toUpperCase(Locale.ROOT);
    }
}