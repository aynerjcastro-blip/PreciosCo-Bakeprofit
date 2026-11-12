package com.comprasco.bakeprofit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.util.NetContentLabel;

class NetContentLabelTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "1000    | G  | 1 kg",
        "500     | G  | 500 g",
        "1500    | ML | 1,5 L",
        "6       | UN | x6",
        "1000.00 | ML | 1 L",
        "2500    | G  | 2,5 kg",
        "1250    | G  | 1,25 kg",
        "999.50  | G  | 999,5 g",
        "250     | ML | 250 ml",
        "10000   | G  | 10 kg"
    })
    void escribeElContenidoComoEnElEmpaque(String content, UnitBase unit, String expected) {
        assertThat(NetContentLabel.format(new BigDecimal(content), unit)).isEqualTo(expected);
    }
}
