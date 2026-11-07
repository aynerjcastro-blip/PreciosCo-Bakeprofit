package com.comprasco.bakeprofit.util;

/**
 * Tipo de código contenido en un GTIN-14, según la regla de GS1 para campos de
 * 14 dígitos. Solo {@link Gtin} lo usa: ninguna otra clase decide comparabilidad
 * a partir de él.
 */
enum GtinKind {
    /** Seis ceros iniciales: un EAN-8 rellenado. */
    GTIN_8,
    /** Empieza por 0: un EAN-13 (o un UPC-A con su 0 delante). */
    GTIN_13,
    /** Indicador 9: producto de medida variable (peso o longitud distinto en cada unidad). */
    VARIABLE_MEASURE,
    /** Indicador 1 a 8: un empaque (caja, paquete) de unidades de consumo. */
    PACKAGING
}