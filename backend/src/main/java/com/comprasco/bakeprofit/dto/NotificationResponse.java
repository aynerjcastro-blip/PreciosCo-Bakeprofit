package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Notification;

import io.swagger.v3.oas.annotations.media.Schema;

/** Aviso de baja de precio visto por su dueño. */
public record NotificationResponse(
    @Schema(description = "Id del aviso")
    Long id,

    @Schema(description = "Id del producto")
    Long productId,

    @Schema(description = "Nombre del producto")
    String productName,

    @Schema(description = "Id de la tienda")
    Long storeId,

    @Schema(description = "Nombre de la tienda")
    String storeName,

    @Schema(description = "Precio anterior", example = "6000")
    BigDecimal previousPrice,

    @Schema(description = "Precio nuevo", example = "4800")
    BigDecimal newPrice,

    @Schema(description = "Si el aviso ya fue leído")
    boolean read,

    @Schema(description = "Fecha del aviso")
    LocalDateTime createdAt
) {
    // Llamar dentro de la transacción del service (relaciones LAZY)
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getProduct().getId(),
            n.getProduct().getName(),
            n.getStore().getId(),
            n.getStore().getName(),
            n.getPreviousValue(),
            n.getNewValue(),
            n.isRead(),
            n.getCreatedAt());
    }
}