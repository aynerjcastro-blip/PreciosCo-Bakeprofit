package com.comprasco.bakeprofit.catalog.dto;

import java.math.BigDecimal;

public record BestSavingsItem(Long id, String name, BigDecimal minPrice, BigDecimal maxPrice,
        BigDecimal savingsPct, int storeCount) {
}
