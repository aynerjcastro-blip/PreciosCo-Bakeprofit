package com.comprasco.bakeprofit.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Notification;

/**
 * Se publica cuando se crea un aviso. Lleva una copia de los datos que necesita quien lo
 * escuche (hoy, el correo), de modo que el listener no depende de la sesión de BD ni de
 * relaciones LAZY: se escucha con {@code @TransactionalEventListener(AFTER_COMMIT)} y el
 * correo solo sale si el aviso quedó guardado.
 */
public record NotificationCreatedEvent(
    Long notificationId,
    String recipientEmail,
    String recipientName,
    String productName,
    String storeName,
    BigDecimal previousPrice,
    BigDecimal newPrice,
    LocalDateTime createdAt
) {
    // Llamar dentro de la transacción del service (relaciones LAZY)
    public static NotificationCreatedEvent from(Notification n) {
        return new NotificationCreatedEvent(
            n.getId(),
            n.getUser().getEmail(),
            n.getUser().getName(),
            n.getProduct().getName(),
            n.getStore().getName(),
            n.getPreviousValue(),
            n.getNewValue(),
            n.getCreatedAt());
    }
}