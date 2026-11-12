package com.comprasco.bakeprofit.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.exception.InvalidSearchFilterException;

class ProductSearchCriteriaTest {

    private static final BigDecimal MAX_PRICE = new BigDecimal("4000");

    @Test
    void lasTiendasRepetidasCuentanUnaVezYLasVaciasSeIgnoran() {
        ProductSearchCriteria criteria =
                ProductSearchCriteria.of(null, null, Arrays.asList(2L, null, 2L, 5L), null, null);

        assertThat(criteria.storeIds()).containsExactlyInAnyOrder(2L, 5L);
    }

    @Test
    void sinTiendaNiRangoNoExigePrecio() {
        ProductSearchCriteria criteria = ProductSearchCriteria.of("arroz", 3L, null, null, null);

        assertThat(criteria.filtersByPrice()).isFalse();
        assertThat(criteria.words()).containsExactly("arroz");
    }

    @Test
    void unaTiendaSinRangoExigePrecioEnEsaTienda() {
        assertThat(ProductSearchCriteria.of(null, null, List.of(1L), null, null).filtersByPrice()).isTrue();
    }

    @Test
    void unRangoSinTiendaExigePrecio() {
        assertThat(ProductSearchCriteria.of(null, null, List.of(), null, MAX_PRICE).filtersByPrice()).isTrue();
    }

    @Test
    void unRangoInvertidoNoCreaLosFiltros() {
        assertThatThrownBy(() -> ProductSearchCriteria.of(null, null, null, MAX_PRICE, BigDecimal.ONE))
                .isInstanceOf(InvalidSearchFilterException.class);
    }
}
