package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;

import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.UnitBase;
import com.comprasco.bakeprofit.util.Gtin;

/**
 * Producto tal como lo ve el cliente. El EAN se guarda como GTIN-14 y se muestra
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
    String manufacturerRef
) {
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
                product.getManufacturerRef()
        );
    }
}