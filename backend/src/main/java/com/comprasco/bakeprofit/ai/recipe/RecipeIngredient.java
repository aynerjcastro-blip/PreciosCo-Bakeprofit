package com.comprasco.bakeprofit.ai.recipe;

import java.util.List;

/**
 * Ingrediente que la IA extrajo del plato pedido.
 *
 * @param name        nombre como se muestra en la receta ("papa criolla")
 * @param quantity    cantidad para la receta ("500 g", "2 unidades")
 * @param searchTerms términos para buscarlo en el catálogo, del más
 *                    específico al más general ("papa criolla", "papa")
 */
public record RecipeIngredient(String name, String quantity, List<String> searchTerms) {
}
