package com.comprasco.bakeprofit.dto;

import com.comprasco.bakeprofit.entity.Store;

public record StoreResponse(
    Long id,
    String name,
    String category
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
            store.getId(),
            store.getName(),
            store.getCategory().getName()
        );
    }
}
