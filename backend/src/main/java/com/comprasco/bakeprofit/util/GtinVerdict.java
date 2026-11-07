package com.comprasco.bakeprofit.util;

/**
 * Resultado de evaluar un código como identidad de producto en PreciosCo
 * ({@link Gtin#assess(String)}). Solo COMPARABLE se puede guardar.
 */
public enum GtinVerdict {
    /** No son solo dígitos, la longitud no es 8, 12, 13 ni 14, o el dígito verificador es incorrecto. */
    INVALID,
    /** Todo ceros: un valor de relleno, no un producto. */
    PLACEHOLDER,
    /** Circulación restringida: lo asigna cada tienda y no identifica el producto entre tiendas. */
    STORE_INTERNAL,
    /** Medida variable: cada unidad pesa distinto, así que su precio no es comparable entre tiendas. */
    VARIABLE_MEASURE,
    /** Identifica el mismo producto en cualquier tienda. Incluye los empaques: son productos distintos de la unidad suelta. */
    COMPARABLE
}