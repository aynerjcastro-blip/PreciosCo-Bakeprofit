package com.comprasco.bakeprofit.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ManufacturerReferenceTest {

    @ParameterizedTest
    @ValueSource(strings = { "ABC-123", "abc 123", "abc-1 23", " a-b c\t1\n2-3 ", "abc.123", "ABC123" })
    void quitaSeparadoresYPasaAMayusculas(String reference) {
        assertThat(ManufacturerReference.normalize(reference)).isEqualTo("ABC123");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "0 451 103 318 | 0451103318",
        "12345.678     | 12345678",
        "A 000/420     | A000420"
    })
    void dejaSoloLetrasYNumeros(String reference, String expected) {
        assertThat(ManufacturerReference.normalize(reference)).isEqualTo(expected);
    }

    @Test
    void eliminaLetrasNoAscii() {
        assertThat(ManufacturerReference.normalize("añ-Á 12")).isEqualTo("A12");
    }

    @Test
    void referenciaNulaDaNulo() {
        assertThat(ManufacturerReference.normalize(null)).isNull();
    }

    @Test
    void referenciaSinLetrasNiNumerosQuedaVacia() {
        assertThat(ManufacturerReference.normalize(" -./ ")).isEmpty();
    }
}