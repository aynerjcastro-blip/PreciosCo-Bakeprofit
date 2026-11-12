package com.comprasco.bakeprofit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.catalog.exception.InvalidSearchFilterException;
import com.comprasco.bakeprofit.price.util.PriceRange;

/** Regla 4 de HU-06b: límites opcionales e inclusivos, sin negativos ni rango invertido. */
class PriceRangeTest {

    private static final BigDecimal TWO_THOUSAND = new BigDecimal("2000");
    private static final BigDecimal FIVE_THOUSAND = new BigDecimal("5000");

    @Test
    void sinLimitesNoFiltra() {
        assertThat(PriceRange.UNBOUNDED.isBounded()).isFalse();
        assertThat(new PriceRange(null, null).isBounded()).isFalse();
    }

    @Test
    void soloElMinimoOSoloElMaximoSonValidos() {
        assertThat(new PriceRange(TWO_THOUSAND, null).isBounded()).isTrue();
        assertThat(new PriceRange(null, FIVE_THOUSAND).isBounded()).isTrue();
    }

    @Test
    void minimoIgualAlMaximoEsValido() {
        assertThat(new PriceRange(TWO_THOUSAND, new BigDecimal("2000.00")).isBounded()).isTrue();
    }

    @Test
    void ceroEsUnLimiteValido() {
        assertThat(new PriceRange(BigDecimal.ZERO, FIVE_THOUSAND).min()).isZero();
    }

    @Test
    void minimoMayorQueMaximoResponde400() {
        assertThatThrownBy(() -> new PriceRange(FIVE_THOUSAND, TWO_THOUSAND))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("mínimo (5000)")
                .hasMessageContaining("máximo (2000)");
    }

    @Test
    void minimoNegativoResponde400() {
        assertThatThrownBy(() -> new PriceRange(new BigDecimal("-1"), null))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("mínimo");
    }

    @Test
    void maximoNegativoResponde400() {
        assertThatThrownBy(() -> new PriceRange(null, new BigDecimal("-0.01")))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessageContaining("máximo");
    }
}
