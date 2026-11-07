package com.comprasco.bakeprofit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GtinTest {

    private static final String EAN_13 = "7702552000141";
    private static final String UPC_A = "036000291452";
    private static final String UPC_A_AS_EAN_13 = "0036000291452";
    private static final String EAN_8 = "96385074";
    private static final String PACKAGING_GTIN_14 = "10012345678902";
    private static final String VARIABLE_MEASURE_GTIN_14 = "90012345678908";

    /* assess: un caso por veredicto */

    @ParameterizedTest
    @ValueSource(strings = { EAN_13, UPC_A, UPC_A_AS_EAN_13, EAN_8, "07702552000141" })
    void codigoDeFabricanteEsComparable(String code) {
        assertThat(Gtin.assess(code)).isEqualTo(GtinVerdict.COMPARABLE);
    }

    @Test
    void empaqueConIndicador1EsComparable() {
        assertThat(Gtin.assess(PACKAGING_GTIN_14)).isEqualTo(GtinVerdict.COMPARABLE);
    }

    @Test
    void digitoVerificadorIncorrectoEsInvalido() {
        assertThat(Gtin.assess("7702552000142")).isEqualTo(GtinVerdict.INVALID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "123", "12345678901", "770255200014A", "123456789012345" })
    void formatoIncorrectoEsInvalido(String code) {
        assertThat(Gtin.assess(code)).isEqualTo(GtinVerdict.INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = { "00000000", "000000000000", "0000000000000", "00000000000000" })
    void puroCerosEsRellenoAunqueSuDigitoVerificadorSeaCorrecto(String zeros) {
        // Se revisa antes de clasificar: si no, saldría como EAN-8 con prefijo 0 (STORE_INTERNAL)
        assertThat(Gtin.assess(zeros)).isEqualTo(GtinVerdict.PLACEHOLDER);
    }

    @ParameterizedTest
    @ValueSource(strings = { "2000123456782", "0412345678903", "01234565", "21234569" })
    void circulacionRestringidaEsDeUsoInterno(String code) {
        assertThat(Gtin.assess(code)).isEqualTo(GtinVerdict.STORE_INTERNAL);
    }

    @ParameterizedTest
    @ValueSource(strings = { "212345678909", "0212345678909" })
    void elMismoCodigoRestringidoSeRechazaConDoceOTreceDigitos(String code) {
        assertThat(Gtin.assess(code)).isEqualTo(GtinVerdict.STORE_INTERNAL);
    }

    @Test
    void ean8RestringidoEscritoConCatorceDigitosEsDeUsoInterno() {
        assertThat(Gtin.assess("00000001234565")).isEqualTo(GtinVerdict.STORE_INTERNAL);
    }

    @Test
    void indicador9EsMedidaVariable() {
        assertThat(Gtin.assess(VARIABLE_MEASURE_GTIN_14)).isEqualTo(GtinVerdict.VARIABLE_MEASURE);
    }

    /* classify */

    @Test
    void clasificaSegunLaReglaDeGs1ParaCatorceDigitos() {
        assertThat(Gtin.classify("00000096385074")).isEqualTo(GtinKind.GTIN_8);
        assertThat(Gtin.classify("07702552000141")).isEqualTo(GtinKind.GTIN_13);
        assertThat(Gtin.classify(VARIABLE_MEASURE_GTIN_14)).isEqualTo(GtinKind.VARIABLE_MEASURE);
        assertThat(Gtin.classify(PACKAGING_GTIN_14)).isEqualTo(GtinKind.PACKAGING);
    }

    @Test
    void clasificarUnCodigoQueNoTieneCatorceDigitosFalla() {
        assertThatThrownBy(() -> Gtin.classify(EAN_13)).isInstanceOf(IllegalArgumentException.class);
    }

    /* normalize */

    @Test
    void upcAQuedaIgualASuEan13Equivalente() {
        assertThat(Gtin.normalize(UPC_A))
                .isEqualTo(Gtin.normalize(UPC_A_AS_EAN_13))
                .isEqualTo("00036000291452");
    }

    @Test
    void normalizarRellenaACatorceONulo() {
        assertThat(Gtin.normalize(EAN_13)).isEqualTo("07702552000141");
        assertThat(Gtin.normalize(null)).isNull();
    }

    @Test
    void normalizarUnFormatoInvalidoFalla() {
        assertThatThrownBy(() -> Gtin.normalize("123")).isInstanceOf(IllegalArgumentException.class);
    }

    /* toDisplay */

    @Test
    void ean8SeMuestraConOchoDigitos() {
        assertThat(Gtin.toDisplay("00000096385074")).isEqualTo(EAN_8);
    }

    @Test
    void ean13SeMuestraConTreceDigitos() {
        assertThat(Gtin.toDisplay("07702552000141")).isEqualTo(EAN_13);
        assertThat(Gtin.toDisplay("00036000291452")).isEqualTo(UPC_A_AS_EAN_13);
    }

    @Test
    void empaqueSeMuestraConCatorceDigitosYNuloQuedaNulo() {
        assertThat(Gtin.toDisplay(PACKAGING_GTIN_14)).isEqualTo(PACKAGING_GTIN_14);
        assertThat(Gtin.toDisplay(null)).isNull();
    }
}