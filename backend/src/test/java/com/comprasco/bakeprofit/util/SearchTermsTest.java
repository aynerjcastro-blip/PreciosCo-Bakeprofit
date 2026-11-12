package com.comprasco.bakeprofit.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.comprasco.bakeprofit.core.utils.SearchTerms;

class SearchTermsTest {

    @Test
    void separaPalabrasSinEspaciosSobrantes() {
        assertThat(SearchTerms.words("  leche   entera ")).containsExactly("leche", "entera");
    }

    @Test
    void textoVacioONuloNoTienePalabras() {
        assertThat(SearchTerms.words(null)).isEmpty();
        assertThat(SearchTerms.words("   ")).isEmpty();
    }

    @Test
    void lasTildesYMayusculasSeConservanParaQueLasQuitePostgres() {
        assertThat(SearchTerms.words("Café")).containsExactly("Café");
    }

    @Test
    void elPatronBuscaLaPalabraEnCualquierParteDelNombre() {
        assertThat(SearchTerms.containsPattern("arroz")).isEqualTo("%arroz%");
    }

    @Test
    void losComodinesDelUsuarioSeBuscanComoTexto() {
        assertThat(SearchTerms.containsPattern("50%")).isEqualTo("%50\\%%");
        assertThat(SearchTerms.containsPattern("a_b")).isEqualTo("%a\\_b%");
        assertThat(SearchTerms.containsPattern("a\\b")).isEqualTo("%a\\\\b%");
    }
}
