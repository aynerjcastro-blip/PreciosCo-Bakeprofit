package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) inmutable que encapsula la información 
 * necesaria para migrar los seguimientos de un producto genérico hacia una categoría específica.
 */
public record CategoryTrackingMigrationDto(

        @NotNull(message = "El ID del usuario es obligatorio")
        Long userId,

        @NotNull(message = "El ID del producto genérico anterior es obligatorio")
        Long oldGenericProductId,

        @NotNull(message = "El ID de la categoría de destino es obligatorio")
        Long targetCategoryId,

        @NotNull(message = "La fecha de migración es obligatoria")
        LocalDateTime migratedAt
) {
}