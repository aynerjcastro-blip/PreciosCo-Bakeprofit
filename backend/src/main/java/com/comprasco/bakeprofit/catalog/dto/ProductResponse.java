package com.comprasco.bakeprofit.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.util.Gtin;
import com.comprasco.bakeprofit.price.dto.ProductOffer;

/**
 * Producto tal como lo ve el cliente. El EAN se guarda como GTIN-14 y se
 * muestra
 * sin el 0 inicial (el EAN-13 del empaque); ver {@link Gtin#toDisplay(String)}.
 */
public record ProductResponse(
        Long id,
        String name,
        String unit,
        Boolean active,
        String category,
        String brand,
        String ean,
        BigDecimal netContent,
        UnitBase unitBase,
        String manufacturerRef,
        // Lo que sigue solo lo llena la búsqueda; en el resto de respuestas es null.
        // Foto del producto en una tienda (la del precio vigente más bajo que tenga
        // foto)
        String imageUrl,
        // Resumen de los precios vigentes en tiendas activas; null si ninguna tienda lo
        // vende
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String minPriceStoreName,
        Integer storeCount,
        // Una oferta por tienda, de la más barata a la más cara
        List<ProductOffer> offers) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getUnit(),
                product.getActive(),
                product.getCategory().getName(),
                product.getBrand() != null ? product.getBrand().getName() : null,
                Gtin.toDisplay(product.getEan()),
                product.getNetContent(),
                product.getUnitBase(),
                product.getManufacturerRef(),
                null, null, null, null, null, null);
    }

    public ProductResponse withImageUrl(String imageUrl) {
        return new ProductResponse(id, name, unit, active, category, brand, ean, netContent, unitBase,
                manufacturerRef, imageUrl, minPrice, maxPrice, minPriceStoreName, storeCount, offers);
    }

    /**
     * Agrega las ofertas vigentes y su resumen. La lista debe venir ordenada de la
     * más barata a
     * la más cara y con una sola oferta por tienda: la primera da el precio mínimo
     * y la tienda
     * más barata, la última el máximo. Sin ofertas, el resumen queda vacío y
     * storeCount en 0.
     */
    public ProductResponse withOffers(List<ProductOffer> offers) {
        if (offers.isEmpty()) {
            return new ProductResponse(id, name, unit, active, category, brand, ean, netContent, unitBase,
                    manufacturerRef, imageUrl, null, null, null, 0, List.of());
        }
        ProductOffer cheapest = offers.get(0);
        ProductOffer dearest = offers.get(offers.size() - 1);
        return new ProductResponse(id, name, unit, active, category, brand, ean, netContent, unitBase,
                manufacturerRef, imageUrl, cheapest.value(), dearest.value(), cheapest.storeName(),
                offers.size(), offers);
    }
}