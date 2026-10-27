package com.comprasco.bakeprofit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReviewRequest(

    @Schema(description = "Id del producto", example = "12")
    @NotNull(message = "El producto es obligatorio")
    @Positive
    Long productId,

    @Schema(description = "Id de la tienda donde opinas del producto", example = "3")
    @NotNull(message = "La tienda es obligatoria")
    @Positive
    Long storeId,

    @Schema(description = "Calificación de 1 a 5 estrellas", example = "4", minimum = "1", maximum = "5")
    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    Integer score,

    @Schema(description = "Comentario (10 a 500 caracteres, sin contar espacios de los extremos)",
            example = "Buen precio, pero casi siempre está agotado")
    @NotBlank(message = "El comentario es obligatorio")
    @Size(min = 10, max = 500, message = "El comentario debe tener entre 10 y 500 caracteres")
    String text
) {
    // trim antes de validar: "   hola   " no cuenta como 10 caracteres
    public ReviewRequest {
        if (text != null) {
            text = text.trim();
        }
    }
}
