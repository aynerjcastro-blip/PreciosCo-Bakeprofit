package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record StoreRequest(
    @Schema(description = "Nombre de la tienda o supermercado", example = "D1")
    @NotBlank(message = "El nombre de la tienda es obligatorio")
    String name
) {}