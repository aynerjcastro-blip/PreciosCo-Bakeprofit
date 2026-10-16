package com.comprasco.bakeprofit.dto;

import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Comment;
import com.comprasco.bakeprofit.entity.Rating;

import io.swagger.v3.oas.annotations.media.Schema;

/** Opinión vista por el público: estrellas y comentario, sin estado interno ni email. */
public record ReviewCommentResponse(
    @Schema(description = "Id de la opinión (se usa para reportar)") Long id,
    @Schema(description = "Id del producto") Long productId,
    @Schema(description = "Nombre del producto") String productName,
    @Schema(description = "Id de la tienda") Long storeId,
    @Schema(description = "Nombre de la tienda") String storeName,
    @Schema(description = "Nombre del autor (nunca el email)") String authorName,
    @Schema(description = "Calificación de 1 a 5") Integer score,
    @Schema(description = "Comentario") String text,
    @Schema(description = "Fecha de registro") LocalDateTime registrationDate
) {
    public static ReviewCommentResponse from(Comment c) {
        Rating r = c.getRating();
        return new ReviewCommentResponse(
            r.getId(),
            r.getProduct().getId(),
            r.getProduct().getName(),
            r.getStore().getId(),
            r.getStore().getName(),
            r.getUser().getName(),
            r.getScore(),
            c.getText(),
            c.getRegistrationDate());
    }
}
