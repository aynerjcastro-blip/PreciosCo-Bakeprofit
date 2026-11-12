package com.comprasco.bakeprofit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.price.entity.Price;
import com.comprasco.bakeprofit.store.entity.Store;

class PriceTest {

    private static final String URL = "https://tienda.co/arroz";
    private static final String IMAGE = "https://tienda.co/arroz.jpg";

    private static Price price(String source) {
        return Price.observed(new Product(), new Store(), new BigDecimal("4500"), source, URL, IMAGE);
    }

    @Test
    void unPeriodoNuevoEmpiezaYSeVeEnElMismoMomento() {
        Price price = price("Arroz Diana 500 g");

        assertThat(price.getLastSeenAt()).isEqualTo(price.getRegistrationDate());
    }

    @Test
    void comparaElValorComoNumero() {
        assertThat(price("Arroz").hasValue(new BigDecimal("4500.00"))).isTrue();
        assertThat(price("Arroz").hasValue(new BigDecimal("4400"))).isFalse();
    }

    @Test
    void recortaLaFuenteALaLongitudDeLaColumnaSinPartirCaracteres() {
        String longName = "a".repeat(Price.SOURCE_MAX_LENGTH - 1) + "😀😀";

        String source = price(longName).getSource();

        assertThat(source.codePointCount(0, source.length())).isEqualTo(Price.SOURCE_MAX_LENGTH);
        assertThat(source).endsWith("😀");
    }

    @Test
    void refrescarNoBorraEnlacesConUnoVacioYNoTocaElValorNiLaFecha() {
        Price price = price("Arroz");
        var registered = price.getRegistrationDate();

        price.refreshSighting(null, " ");

        assertThat(price.getProductUrl()).isEqualTo(URL);
        assertThat(price.getImageUrl()).isEqualTo(IMAGE);
        assertThat(price.getRegistrationDate()).isEqualTo(registered);
        assertThat(price.getValue()).isEqualByComparingTo("4500");
        assertThat(price.getLastSeenAt()).isAfterOrEqualTo(registered);
    }

    @Test
    void refrescarReemplazaLosEnlacesCuandoLaOfertaTraeOtros() {
        Price price = price("Arroz");

        price.refreshSighting("https://tienda.co/nuevo", "https://tienda.co/nuevo.jpg");

        assertThat(price.getProductUrl()).isEqualTo("https://tienda.co/nuevo");
        assertThat(price.getImageUrl()).isEqualTo("https://tienda.co/nuevo.jpg");
    }
}
