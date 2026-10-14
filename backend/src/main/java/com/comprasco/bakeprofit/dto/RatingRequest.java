package com.comprasco.bakeprofit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RatingRequest(

    @Schema(description = "Id del producto que se califica", example = "12")
    @NotNull(message = "El producto es obligatorio")
    @Positive
    Long productId,

    @Schema(description = "Id de la tienda donde se califica el producto", example = "3")
    @NotNull(message = "La tienda es obligatoria")
    @Positive
    Long storeId,

    @Schema(description = "Puntaje de 1 a 5 estrellas", example = "4", minimum = "1", maximum = "5")
    @NotNull(message = "El puntaje es obligatorio")
    @Min(value = 1, message = "El puntaje mínimo es 1")
    @Max(value = 5, message = "El puntaje máximo es 5")
    Integer score
) {}