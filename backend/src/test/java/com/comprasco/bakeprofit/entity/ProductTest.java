package com.comprasco.bakeprofit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;

class ProductTest {

    private static final String GTIN = "07702552000141";

    @Test
    void elNombreQueTraeLaTiendaSeRecortaALaColumna() {
        String longName = "Arroz ".repeat(60);

        Product product = Product.withIdentity(longName, null, null, GTIN, new BigDecimal("500"), UnitBase.G);

        assertThat(product.getName()).hasSize(Product.NAME_MAX_LENGTH);
        assertThat(longName).startsWith(product.getName());
    }

    @Test
    void unNombreQueCabeNoCambia() {
        Product product = Product.withIdentity("Arroz Diana 500 g", null, null, GTIN,
                new BigDecimal("500"), UnitBase.G);

        assertThat(product.getName()).isEqualTo("Arroz Diana 500 g");
    }
}
