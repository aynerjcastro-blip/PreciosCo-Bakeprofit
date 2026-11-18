package com.comprasco.bakeprofit.staging.identity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class NormalizedProductNameSimilarityTest {
    private final ProductNameSimilarity similarity = new NormalizedProductNameSimilarity();

    @Test
    void ordenTildesYUnidadesNoCambianElNombreNormalizado() {
        assertThat(similarity.score("Cetaphil Hidratación Facial x 50 Ml",
                "HIDRATACION CETAPHIL 50 ml")).isEqualTo(1);
    }

    @Test
    void conservaSpfConcentracionYVarianteAunqueElNombreSeaMuyParecido() {
        assertThat(similarity.score("Cetaphil Healthy Renew SPF 30 50 ml",
                "Cetaphil Healthy Renew SPF 50 50 ml")).isZero();
        assertThat(similarity.score("Acido salicilico 2% 50 ml", "Acido salicilico 1% 50 ml")).isZero();
        assertThat(similarity.score("Cetaphil Healthy Renew noche 50 ml",
                "Cetaphil Healthy Renew dia 50 ml")).isZero();
        assertThat(similarity.score("Crema facial 50 ml", "Crema facial 50 ml")).isZero();
    }
}
