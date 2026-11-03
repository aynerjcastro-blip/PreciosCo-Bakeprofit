package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Follow;

import io.swagger.v3.oas.annotations.media.Schema;

/** Seguimiento visto por su dueño. */
public record FollowResponse(
    @Schema(description = "Id del seguimiento")
    Long id,

    @Schema(description = "Id del producto")
    Long productId,

    @Schema(description = "Nombre del producto")
    String productName,

    @Schema(description = "Precio objetivo; null = cualquier reducción", nullable = true)
    BigDecimal targetPrice,

    @Schema(description = "Si el aviso está activo")
    Boolean notificationActive,

    @Schema(description = "Precio más bajo vigente entre tiendas; null si no hay precios", nullable = true)
    BigDecimal lowestPrice,

    @Schema(description = "Fecha en que empezó a seguirlo")
    LocalDateTime registrationDate
) {
    // Llamar dentro de la transacción del service (relaciones LAZY)
    public static FollowResponse from(Follow follow, BigDecimal lowestPrice) {
        return new FollowResponse(
            follow.getId(),
            follow.getProduct().getId(),
            follow.getProduct().getName(),
            follow.getTargetPrice(),
            follow.getNotificationActive(),
            lowestPrice,
            follow.getRegistrationDate());
    }
}