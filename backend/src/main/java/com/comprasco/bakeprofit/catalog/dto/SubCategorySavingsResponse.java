package com.comprasco.bakeprofit.catalog.dto;

import java.util.List;

public record SubCategorySavingsResponse(
        Long subcategoryId,
        String subcategoryName,
        List<ProductResponse> products) {
}