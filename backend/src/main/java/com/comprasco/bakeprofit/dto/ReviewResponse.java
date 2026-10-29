package com.comprasco.bakeprofit.dto;

import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Comment;
import com.comprasco.bakeprofit.entity.Rating;

import io.swagger.v3.oas.annotations.media.Schema;

/** Opinión vista por su autor (crear, editar y consultar la propia). */
public record ReviewResponse(
    @Schema(description = "Id de la opinión (el de la calificación)") 
    Long id,

    @Schema(description = "Id del producto") 
    Long productId,

    @Schema(description = "Id de la tienda") 
    Long storeId,

    @Schema(description = "Calificación de 1 a 5") 
    Integer score,

    @Schema(description = "Comentario") 
    String text,
    
    @Schema(description = "Estado del comentario (ELIMINADO: no se puede volver a comentar)") 
    Comment.CommentStatus commentStatus,

    @Schema(description = "Fecha de registro") 
    LocalDateTime registrationDate,
    
    @Schema(description = "Hasta cuándo se puede editar; null si ya no es editable") 
    LocalDateTime editableUntil
) {
    // Llamar dentro de la transacción del service (relaciones LAZY)
    public static ReviewResponse from(Rating rating, Comment comment, LocalDateTime editableUntil) {
        return new ReviewResponse(
            rating.getId(),
            rating.getProduct().getId(),
            rating.getStore().getId(),
            rating.getScore(),
            comment.getText(),
            comment.getStatus(),
            rating.getRegistrationDate(),
            editableUntil);
    }
}
