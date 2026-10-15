package com.comprasco.bakeprofit.dto;

import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Comment;

import io.swagger.v3.oas.annotations.media.Schema;

public record CommentResponse(
    @Schema(description = "Id del comentario") Long id,
    @Schema(description = "Id del producto") Long productId,
    @Schema(description = "Nombre del producto") String productName,
    @Schema(description = "Id de la tienda") Long storeId,
    @Schema(description = "Nombre de la tienda") String storeName,
    @Schema(description = "Nombre del autor (nunca el email)") String authorName,
    @Schema(description = "Texto del comentario") String text,
    @Schema(description = "Fecha de registro") LocalDateTime registrationDate
) {
    // Llamar dentro de la transacción del service (relaciones LAZY)
    public static CommentResponse from(Comment c) {
        return new CommentResponse(
            c.getId(),
            c.getProduct().getId(),
            c.getProduct().getName(),
            c.getStore().getId(),
            c.getStore().getName(),
            c.getUser().getName(),
            c.getText(),
            c.getRegistrationDate());
    }
}
