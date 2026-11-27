package com.comprasco.bakeprofit.ai.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class RecipeExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesDishServingsAndIngredients() throws Exception {
        ExtractedRecipe recipe = RecipeExtractor.parse(json("""
                {"isRecipe": true, "dish": "Ajiaco", "servings": 4, "ingredients": [
                  {"name": "papa criolla", "quantity": "500 g", "searchTerms": ["papa criolla", "papa"]},
                  {"name": "pollo", "quantity": "1 kg", "searchTerms": ["pechuga pollo", "pollo"]}
                ]}"""));

        assertThat(recipe.recipe()).isTrue();
        assertThat(recipe.dish()).isEqualTo("Ajiaco");
        assertThat(recipe.servings()).isEqualTo(4);
        assertThat(recipe.ingredients()).extracting(RecipeIngredient::name).containsExactly("papa criolla", "pollo");
        assertThat(recipe.ingredients().getFirst().searchTerms()).containsExactly("papa criolla", "papa");
    }

    @Test
    void notARecipeWhenModelSaysSoOrThereAreNoIngredients() throws Exception {
        assertThat(RecipeExtractor.parse(json("""
                {"isRecipe": false, "dish": "", "servings": 0, "ingredients": []}""")).recipe()).isFalse();
        assertThat(RecipeExtractor.parse(json("""
                {"isRecipe": true, "dish": "Nada", "servings": 2, "ingredients": []}""")).recipe()).isFalse();
    }

    @Test
    void sanitizesLimitsBlankNamesAndMissingTerms() throws Exception {
        StringBuilder many = new StringBuilder("{\"isRecipe\": true, \"dish\": \"X\", \"servings\": 2, \"ingredients\": [");
        many.append("{\"name\": \"  \", \"quantity\": \"\", \"searchTerms\": []},");
        many.append("{\"name\": \"sal\", \"quantity\": \"1 pizca\", \"searchTerms\": []}");
        for (int i = 0; i < 20; i++) {
            many.append(",{\"name\": \"ing").append(i).append("\", \"quantity\": \"1\", \"searchTerms\": [\"a\",\"b\",\"c\",\"d\"]}");
        }
        many.append("]}");

        ExtractedRecipe recipe = RecipeExtractor.parse(json(many.toString()));

        assertThat(recipe.ingredients()).hasSize(RecipeExtractor.MAX_INGREDIENTS);
        // El nombre vacío se descarta y sin términos se usa el nombre
        assertThat(recipe.ingredients().getFirst().name()).isEqualTo("sal");
        assertThat(recipe.ingredients().getFirst().searchTerms()).containsExactly("sal");
        // Máximo 3 términos por ingrediente
        assertThat(recipe.ingredients().get(1).searchTerms()).hasSize(3);
    }

    private JsonNode json(String text) throws Exception {
        return mapper.readTree(text);
    }
}
