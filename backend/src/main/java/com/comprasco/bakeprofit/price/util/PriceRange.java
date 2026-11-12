package com.comprasco.bakeprofit.price.util;

import java.math.BigDecimal;

import com.comprasco.bakeprofit.catalog.exception.InvalidSearchFilterException;

/**
 * Rango de precio de la búsqueda (HU-06b). Los dos límites son opcionales e inclusivos.
 *
 * <p>
 * El rango se valida al crearse: un rango que existe siempre es válido, así que quien lo
 * recibe no vuelve a comprobarlo. Un precio de 0 es un límite válido; uno negativo no.
 * </p>
 */
public record PriceRange(BigDecimal min, BigDecimal max) {

    /** Sin límites: no filtra por precio. */
    public static final PriceRange UNBOUNDED = new PriceRange(null, null);

    private static final String NEGATIVE_LIMIT_MESSAGE =
            "El precio %s no puede ser negativo: %s.";
    private static final String INVERTED_RANGE_MESSAGE =
            "El precio mínimo (%s) no puede ser mayor que el máximo (%s).";

    public PriceRange {
        rejectNegative("mínimo", min);
        rejectNegative("máximo", max);
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new InvalidSearchFilterException(
                    INVERTED_RANGE_MESSAGE.formatted(min.toPlainString(), max.toPlainString()));
        }
    }

    public boolean isBounded() {
        return min != null || max != null;
    }

    private static void rejectNegative(String limitName, BigDecimal limit) {
        if (limit != null && limit.signum() < 0) {
            throw new InvalidSearchFilterException(
                    NEGATIVE_LIMIT_MESSAGE.formatted(limitName, limit.toPlainString()));
        }
    }
}
