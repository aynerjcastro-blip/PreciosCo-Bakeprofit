package com.comprasco.bakeprofit.ai.recipe;

import java.util.List;

import com.comprasco.bakeprofit.price.dto.CatalogOfferRow;

/**
 * Ingrediente de la receta cruzado con el catálogo.
 *
 * @param ingredient ingrediente pedido
 * @param products   productos del catálogo que lo cubren, del más relevante
 *                   al menos (vacío = no disponible en el catálogo)
 */
public record MatchedIngredient(RecipeIngredient ingredient, List<ProductMatch> products) {

    public boolean available() {
        return !products.isEmpty();
    }

    /** El producto más relevante (nombre más corto que contiene el término). */
    public ProductMatch best() {
        return products.isEmpty() ? null : products.getFirst();
    }

    /**
     * Producto del catálogo con sus precios vigentes.
     *
     * @param offers precios vigentes por tienda, del más barato al más caro
     */
    public record ProductMatch(Long productId, String name, String unit, String brandName,
            List<CatalogOfferRow> offers) {

        public CatalogOfferRow cheapest() {
            return offers.getFirst();
        }
    }
}
