package com.comprasco.bakeprofit.ai.recipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.ai.client.GeminiClient;
import com.comprasco.bakeprofit.ai.dto.AiMessage;
import com.comprasco.bakeprofit.ai.prompt.SectionPrompt;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Primer paso de la receta (SCRUM-325): pide a Gemini, con salida JSON
 * estructurada, el plato y la lista de ingredientes con términos de búsqueda.
 */
@Component
public class RecipeExtractor {

    static final int MAX_INGREDIENTS = 15;
    private static final int MAX_TERMS = 3;
    private static final int MAX_TEXT = 80;

    /** Esquema de la respuesta (formato OpenAPI que acepta Gemini). */
    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "isRecipe", Map.of("type", "BOOLEAN"),
                    "dish", Map.of("type", "STRING"),
                    "servings", Map.of("type", "INTEGER"),
                    "ingredients", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "name", Map.of("type", "STRING"),
                                            "quantity", Map.of("type", "STRING"),
                                            "searchTerms", Map.of(
                                                    "type", "ARRAY",
                                                    "items", Map.of("type", "STRING"))),
                                    "required", List.of("name", "quantity", "searchTerms")))),
            "required", List.of("isRecipe", "dish", "servings", "ingredients"));

    private final GeminiClient geminiClient;

    public RecipeExtractor(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * @param messages historial + mensaje actual (para entender "y para 6
     *                 personas?" en una conversación)
     */
    public ExtractedRecipe extract(List<AiMessage> messages) throws IOException, InterruptedException {
        JsonNode json = geminiClient.generateJson(SectionPrompt.RECIPE_EXTRACTION, messages, SCHEMA);
        return parse(json);
    }

    /** Convierte y sanea la respuesta: límites de cantidad y longitud, sin vacíos. */
    static ExtractedRecipe parse(JsonNode json) {
        boolean isRecipe = json.path("isRecipe").asBoolean(false);
        String dish = clip(json.path("dish").asText(""));
        int servings = Math.max(0, json.path("servings").asInt(0));

        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (JsonNode node : json.path("ingredients")) {
            if (ingredients.size() >= MAX_INGREDIENTS) {
                break;
            }
            String name = clip(node.path("name").asText(""));
            if (name.isEmpty()) {
                continue;
            }
            List<String> terms = new ArrayList<>();
            for (JsonNode term : node.path("searchTerms")) {
                String t = clip(term.asText(""));
                if (!t.isEmpty() && terms.size() < MAX_TERMS) {
                    terms.add(t);
                }
            }
            if (terms.isEmpty()) {
                terms.add(name);
            }
            ingredients.add(new RecipeIngredient(name, clip(node.path("quantity").asText("")), terms));
        }

        return new ExtractedRecipe(isRecipe && !ingredients.isEmpty(), dish, servings, ingredients);
    }

    private static String clip(String text) {
        String t = text == null ? "" : text.strip();
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
    }
}
