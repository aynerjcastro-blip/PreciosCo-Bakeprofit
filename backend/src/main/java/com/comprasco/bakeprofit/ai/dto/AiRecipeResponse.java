package com.comprasco.bakeprofit.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento final ({@code done}) del stream de recetas (SCRUM-325). Lleva la
 * disponibilidad de los ingredientes ya resuelta contra el catálogo para que el
 * frontend la muestre sin tener que leerla del texto.
 *
 * @param recipesUsed    recetas pedidas este mes (después de esta)
 * @param recipesLimit   límite mensual de recetas (0 = ilimitado / PREMIUM)
 * @param plan           "FREE" o "PREMIUM"
 * @param dish           nombre del plato
 * @param servings       porciones
 * @param ingredients    ingredientes en el orden de la receta
 * @param estimatedTotal suma del precio más barato de cada producto disponible
 *                       (precio del producto, no proporcional a la cantidad)
 */
public record AiRecipeResponse(
        int recipesUsed,
        int recipesLimit,
        String plan,
        String dish,
        int servings,
        List<Ingredient> ingredients,
        BigDecimal estimatedTotal) {

    /**
     * Un ingrediente y, si está disponible, el producto recomendado del
     * catálogo con su mejor precio.
     *
     * @param url página del producto en la tienda (null si no hay)
     */
    public record Ingredient(
            String name,
            String quantity,
            boolean available,
            String productName,
            String storeName,
            BigDecimal price,
            String url) {
    }
}
