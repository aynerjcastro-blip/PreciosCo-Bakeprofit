package com.comprasco.bakeprofit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición dentro del plazo: calificación y comentario siempre juntos (RN-30). */
public record ReviewUpdateRequest(

    @Schema(description = "Calificación de 1 a 5 estrellas", example = "5", minimum = "1", maximum = "5")
    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    Integer score,

    @Schema(description = "Comentario (10 a 500 caracteres)", example = "Actualizo: ya lo tienen disponible")
    @NotBlank(message = "El comentario es obligatorio")
    @Size(min = 10, max = 500, message = "El comentario debe tener entre 10 y 500 caracteres")
    String text
) {
    public ReviewUpdateRequest {
        if (text != null) {
            text = text.trim();
        }
    }
}
