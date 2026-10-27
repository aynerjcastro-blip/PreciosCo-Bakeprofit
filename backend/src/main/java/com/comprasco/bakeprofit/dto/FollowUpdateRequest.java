package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FollowUpdateRequest(

    @Schema(description = "true = avisar; false = pausar el aviso sin perder el seguimiento", example = "true")
    @NotNull(message = "Indica si el aviso está activo")
    Boolean notificationActive,

    @Schema(description = "Precio objetivo en pesos; null lo quita (aviso ante cualquier reducción)",
            example = "5000", nullable = true)
    @Positive(message = "El precio objetivo debe ser mayor que cero")
    @Digits(integer = 10, fraction = 2, message = "El precio objetivo tiene un formato inválido")
    BigDecimal targetPrice
) {
}