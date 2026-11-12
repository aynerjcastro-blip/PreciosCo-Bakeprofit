package com.comprasco.bakeprofit.catalog.util;

import java.math.BigDecimal;
import java.util.Objects;

import com.comprasco.bakeprofit.catalog.entity.UnitBase;

/**
 * Texto del campo {@code unit} de un producto a partir de su contenido neto:
 * 1000 G → "1 kg", 500 G → "500 g", 1500 ML → "1,5 L", 6 UN → "x6".
 *
 * <p>
 * Desde 1000 g o 1000 ml se pasa a kg o L, como se escribe en los empaques. El número
 * usa coma decimal (es-CO) y no lleva ceros sobrantes.
 * </p>
 */
public final class NetContentLabel {

    private static final BigDecimal LARGER_UNIT_THRESHOLD = new BigDecimal("1000");
    private static final int LARGER_UNIT_DECIMAL_SHIFT = 3;
    private static final char DECIMAL_POINT = '.';
    private static final char DECIMAL_COMMA = ',';
    private static final String GRAMS = "g";
    private static final String KILOGRAMS = "kg";
    private static final String MILLILITERS = "ml";
    private static final String LITERS = "L";
    private static final String UNITS_PREFIX = "x";
    private static final String SEPARATOR = " ";

    private NetContentLabel() {
    }

    public static String format(BigDecimal netContent, UnitBase unitBase) {
        Objects.requireNonNull(netContent);
        Objects.requireNonNull(unitBase);
        return switch (unitBase) {
            case G -> withLargerUnit(netContent, GRAMS, KILOGRAMS);
            case ML -> withLargerUnit(netContent, MILLILITERS, LITERS);
            case UN -> UNITS_PREFIX + number(netContent);
        };
    }

    private static String withLargerUnit(BigDecimal amount, String baseSymbol, String largerSymbol) {
        if (amount.compareTo(LARGER_UNIT_THRESHOLD) >= 0) {
            return number(amount.movePointLeft(LARGER_UNIT_DECIMAL_SHIFT)) + SEPARATOR + largerSymbol;
        }
        return number(amount) + SEPARATOR + baseSymbol;
    }

    private static String number(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString().replace(DECIMAL_POINT, DECIMAL_COMMA);
    }
}
