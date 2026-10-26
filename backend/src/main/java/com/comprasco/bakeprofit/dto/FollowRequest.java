package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FollowRequest(

    @Schema(description = "Id del producto a seguir", example = "12")
    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El id del producto debe ser positivo")
    Long productId,

    @Schema(description = "Precio objetivo en pesos. Si se omite, se avisa ante cualquier reducción",
            example = "5000", nullable = true)
    @Positive(message = "El precio objetivo debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El precio objetivo tiene un formato inválido")
    BigDecimal targetPrice
) {
}