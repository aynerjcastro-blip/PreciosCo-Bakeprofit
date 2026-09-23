package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record CategoryRequest(
    @Schema(description = "Nombre a asignar a la categoria")
    @NotBlank(message = "El nombre de la categoria es obligatorio")
    String name
) {}