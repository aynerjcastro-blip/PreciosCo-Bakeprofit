package com.comprasco.bakeprofit.ai.recipe;

import java.util.List;

/**
 * Resultado del primer paso de la receta: qué plato pidió el usuario y qué
 * ingredientes necesita.
 *
 * @param recipe      false si el mensaje no pide un plato o receta
 * @param dish        nombre del plato ("Ajiaco santafereño")
 * @param servings    porciones (0 si no se pudo deducir)
 * @param ingredients ingredientes con sus términos de búsqueda
 */
public record ExtractedRecipe(boolean recipe, String dish, int servings, List<RecipeIngredient> ingredients) {
}
