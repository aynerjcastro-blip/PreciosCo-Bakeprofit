package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductRequest(
    @Schema(description = "Nombre del producto", example = "Maiz crispeta 1kg")
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    @Schema(description = "Cantidad de contenido del producto", example = "x5 o 100g o 250ml")
    @NotBlank(message = "La cantidad de unidades es obligatoria")
    String unit,

    @Schema(description = "Id de la categoría a la que pertenece", example = "1")
    @NotNull(message = "La categoria es obligatoria")
    Long idCategory
) {}