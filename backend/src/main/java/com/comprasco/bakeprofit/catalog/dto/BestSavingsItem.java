package com.comprasco.bakeprofit.catalog.dto;

import java.math.BigDecimal;

public record BestSavingsItem(Long id, String name, BigDecimal minPrice, BigDecimal maxPrice,
                BigDecimal savingsPct, int storeCount, String imageUrl) {

        public BestSavingsItem(Long id, String name, BigDecimal minPrice, BigDecimal maxPrice,
                        BigDecimal savingsPct, int storeCount) {
                this(id, name, minPrice, maxPrice, savingsPct, storeCount, null);
        }

        /** Devuelve una copia con la imagen inyectada. */
        public BestSavingsItem withImage(String url) {
                return new BestSavingsItem(id, name, minPrice, maxPrice, savingsPct, storeCount, url);
        }
}