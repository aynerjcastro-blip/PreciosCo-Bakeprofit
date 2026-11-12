package com.comprasco.bakeprofit.catalog.dto;

import java.math.BigDecimal;

import com.comprasco.bakeprofit.catalog.entity.Product;
import com.comprasco.bakeprofit.catalog.entity.UnitBase;
import com.comprasco.bakeprofit.catalog.util.Gtin;

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
        // Foto del producto en una tienda; solo la trae la búsqueda (null en el resto
        // de respuestas)
        String imageUrl) {
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
                null);
    }

    public ProductResponse withImageUrl(String imageUrl) {
        return new ProductResponse(id, name, unit, active, category, brand, ean, netContent, unitBase,
                manufacturerRef, imageUrl);
    }
}