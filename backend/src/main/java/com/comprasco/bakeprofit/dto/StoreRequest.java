package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StoreRequest(
    @Schema(description = "Nombre de la tienda o supermercado", example = "D1")
    @NotBlank(message = "El nombre de la tienda es obligatorio")
    String name,

    @Schema(description = "Id de la categoría a la que pertenece", example = "1")
    @NotNull(message = "La categoría de la tienda es obligatoria")
    Long categoryId
) {}