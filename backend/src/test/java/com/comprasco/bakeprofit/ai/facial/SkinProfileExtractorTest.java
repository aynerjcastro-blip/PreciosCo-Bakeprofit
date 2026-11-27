package com.comprasco.bakeprofit.ai.facial;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** HU-40: saneamiento del perfil de piel que extrae la IA. */
class SkinProfileExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesSkinTypeNeedsBudgetAndOrdersTypesAsARoutine() throws Exception {
        SkinProfile p = SkinProfileExtractor.parse(json("""
                {"isFacialRequest": true, "skinType": "grasa", "needs": ["acne", "brillo"], "budget": 60000,
                 "productTypes": ["protector_solar", "limpiador", "serum"]}"""));

        assertThat(p.facialRequest()).isTrue();
        assertThat(p.skinType()).isEqualTo("grasa");
        assertThat(p.needs()).containsExactly("acne", "brillo");
        assertThat(p.budget()).isEqualByComparingTo(new BigDecimal("60000"));
        assertThat(p.productTypes()).containsExactly(
                FacialProductType.LIMPIADOR, FacialProductType.SERUM, FacialProductType.PROTECTOR_SOLAR);
    }

    @Test
    void unknownSkinTypeIsEmpty_soTheAssistantAsks() throws Exception {
        SkinProfile p = SkinProfileExtractor.parse(json("""
                {"isFacialRequest": true, "skinType": "desconocido", "needs": [], "budget": 0, "productTypes": []}"""));

        assertThat(p.hasSkinType()).isFalse();
    }

    @Test
    void zeroOrNegativeBudgetMeansNoBudget() throws Exception {
        assertThat(SkinProfileExtractor.parse(json("""
                {"isFacialRequest": true, "skinType": "seca", "needs": [], "budget": 0, "productTypes": []}""")).budget())
                .isNull();
        assertThat(SkinProfileExtractor.parse(json("""
                {"isFacialRequest": true, "skinType": "seca", "needs": [], "budget": -5, "productTypes": []}""")).budget())
                .isNull();
    }

    @Test
    void valuesOutsideTheListsAreDropped_andEmptyTypesFallBackToBasicRoutine() throws Exception {
        SkinProfile p = SkinProfileExtractor.parse(json("""
                {"isFacialRequest": true, "skinType": "rara", "needs": ["acne", "volar"], "budget": 50000,
                 "productTypes": ["mascarilla"]}"""));

        assertThat(p.skinType()).isEmpty();
        assertThat(p.needs()).containsExactly("acne");
        assertThat(p.productTypes()).isEqualTo(SkinProfileExtractor.BASIC_ROUTINE);
    }

    @Test
    void notAFacialRequest() throws Exception {
        assertThat(SkinProfileExtractor.parse(json("""
                {"isFacialRequest": false, "skinType": "desconocido", "needs": [], "budget": 0,
                 "productTypes": []}""")).facialRequest()).isFalse();
    }

    private JsonNode json(String text) throws Exception {
        return mapper.readTree(text);
    }
}
