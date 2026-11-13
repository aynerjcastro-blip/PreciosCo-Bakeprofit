package com.comprasco.bakeprofit.catalog.dto;

import java.util.List;

public record BestSavingsGroup(Long sectionId, String sectionName, Long categoryId,
        String categoryName, List<BestSavingsItem> products) {
}