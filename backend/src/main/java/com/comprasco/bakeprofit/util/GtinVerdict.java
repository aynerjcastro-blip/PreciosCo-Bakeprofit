package com.comprasco.bakeprofit.util;

import java.util.Optional;

/**
 * Resultado de evaluar un código como identidad de producto en PreciosCo
 * ({@link Gtin#assess(String)}). Solo COMPARABLE se puede guardar.
 *
 * <p>
 * Cada veredicto de rechazo lleva su mensaje: lo usan tanto el registro manual de
 * productos como el procesador de staging, y así un veredicto nuevo no compila sin
 * decir por qué rechaza.
 * </p>
 */
public enum GtinVerdict {
    /** No son solo dígitos, la longitud no es 8, 12, 13 ni 14, o el dígito verificador es incorrecto. */
    INVALID("El código %s no es un EAN válido: revisa el dígito verificador"),
    /** Todo ceros: un valor de relleno, no un producto. */
    PLACEHOLDER("El código %s es un valor de relleno y no corresponde a un producto real"),
    /** Circulación restringida: lo asigna cada tienda y no identifica el producto entre tiendas. */
    STORE_INTERNAL("El código %s es de uso interno de una tienda y no identifica el producto entre tiendas"),
    /** Medida variable: cada unidad pesa distinto, así que su precio no es comparable entre tiendas. */
    VARIABLE_MEASURE("El código %s corresponde a un producto de peso variable "
            + "y su precio no es comparable entre tiendas"),
    /** Identifica el mismo producto en cualquier tienda. Incluye los empaques: son productos distintos de la unidad suelta. */
    COMPARABLE(null);

    private final String rejectionTemplate;

    GtinVerdict(String rejectionTemplate) {
        this.rejectionTemplate = rejectionTemplate;
    }

    /** Motivo del rechazo con el código tal como llegó; vacío si el código es COMPARABLE. */
    public Optional<String> rejectionMessage(String code) {
        return Optional.ofNullable(rejectionTemplate).map(template -> template.formatted(code));
    }
}