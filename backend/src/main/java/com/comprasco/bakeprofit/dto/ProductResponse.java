package com.comprasco.bakeprofit.dto;

import com.comprasco.bakeprofit.entity.Product;

public record ProductResponse(
    Long id,
    String name,
    String unit,
    Boolean active,
    String category
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getUnit(),
                product.getActive(),
                product.getCategory().getName()
        );
    }
}