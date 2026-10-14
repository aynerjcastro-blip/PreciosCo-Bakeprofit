package com.comprasco.bakeprofit.dto;

import java.time.LocalDateTime;

import com.comprasco.bakeprofit.entity.Rating;

import io.swagger.v3.oas.annotations.media.Schema;

public record RatingResponse(
    @Schema(description = "Id de la calificación creada") Long id,
    @Schema(description = "Id del producto calificado") Long productId,
    @Schema(description = "Id de la tienda donde se calificó") Long storeId,
    @Schema(description = "Puntaje guardado (1 a 5)") Integer score,
    @Schema(description = "Fecha de registro asignada por el servidor") LocalDateTime registrationDate
) {
    public static RatingResponse from(Rating r) {
        return new RatingResponse(
            r.getId(),
            r.getProduct().getId(),
            r.getStore().getId(),
            r.getScore(),
            r.getRegistrationDate());
    }
}