package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) inmutable que representa 
 * la estructura de la alerta cuando se detecta una caída de precio en un producto.
 * Utiliza validaciones de Jakarta para asegurar la integridad de los datos en entornos empresariales.
 */
public record PriceDropAlertDto(

        @NotNull(message = "El ID del producto es obligatorio")
        Long productId,

        @NotBlank(message = "El nombre del producto es obligatorio")
        String productName,

        @NotNull(message = "El precio anterior es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio anterior debe ser mayor que cero")
        Double previousPrice,

        @NotNull(message = "El precio actual es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio actual debe ser mayor que cero")
        Double currentPrice,

        @NotNull(message = "La fecha de detección es obligatoria")
        LocalDateTime detectedAt
) {
}