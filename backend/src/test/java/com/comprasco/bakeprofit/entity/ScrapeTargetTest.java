package com.comprasco.bakeprofit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.staging.entity.ScrapeTarget;
import com.comprasco.bakeprofit.store.entity.Store;

class ScrapeTargetTest {

    private static final BigDecimal MIN = new BigDecimal("400");
    private static final BigDecimal MAX = new BigDecimal("1000");

    private static ScrapeTarget target(BigDecimal min, BigDecimal max) {
        return ScrapeTarget.of(new Store(), new Category(), "arroz", UnitBase.G, min, max);
    }

    @Test
    void aceptaContenidoDentroDelRangoIncluyendoLosLimites() {
        ScrapeTarget target = target(MIN, MAX);

        assertThat(target.accepts(MIN, UnitBase.G)).isTrue();
        assertThat(target.accepts(new BigDecimal("500"), UnitBase.G)).isTrue();
        assertThat(target.accepts(new BigDecimal("1000.00"), UnitBase.G)).isTrue();
    }

    @Test
    void rechazaContenidoFueraDelRango() {
        ScrapeTarget target = target(MIN, MAX);

        assertThat(target.accepts(new BigDecimal("399.99"), UnitBase.G)).isFalse();
        assertThat(target.accepts(new BigDecimal("2500"), UnitBase.G)).isFalse();
    }

    @Test
    void rechazaOtraUnidadAunqueElNumeroEsteEnRango() {
        assertThat(target(MIN, MAX).accepts(new BigDecimal("500"), UnitBase.ML)).isFalse();
    }

    @Test
    void rechazaContenidoDesconocido() {
        assertThat(target(MIN, MAX).accepts(null, UnitBase.G)).isFalse();
    }

    @Test
    void unLimiteNuloDejaEseLadoAbierto() {
        assertThat(target(null, MAX).accepts(BigDecimal.ONE, UnitBase.G)).isTrue();
        assertThat(target(MIN, null).accepts(new BigDecimal("50000"), UnitBase.G)).isTrue();
        assertThat(target(null, null).accepts(BigDecimal.ONE, UnitBase.G)).isTrue();
    }
}