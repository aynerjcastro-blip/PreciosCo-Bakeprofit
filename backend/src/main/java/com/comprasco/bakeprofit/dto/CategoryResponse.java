package com.comprasco.bakeprofit.dto;

import com.comprasco.bakeprofit.entity.Category;

public record CategoryResponse(
    Long id, 
    String name, 
    Boolean active, 
    Long parentId, 
    String parentName
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(), category.getName(), category.getActive(),
            category.getParent() != null ? category.getParent().getId() : null,
            category.getParent() != null ? category.getParent().getName() : null
        );
    }
}