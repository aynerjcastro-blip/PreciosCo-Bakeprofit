package com.comprasco.bakeprofit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CommentRequest(

    @Schema(description = "Id del producto comentado", example = "12")
    @NotNull(message = "El producto es obligatorio")
    @Positive
    Long productId,

    @Schema(description = "Id de la tienda donde se comenta el producto", example = "3")
    @NotNull(message = "La tienda es obligatoria")
    @Positive
    Long storeId,

    @Schema(description = "Texto del comentario (10 a 500 caracteres, sin contar espacios de los extremos)",
            example = "Buen precio, pero casi siempre está agotado")
    @NotBlank(message = "El comentario es obligatorio")
    @Size(min = 10, max = 500, message = "El comentario debe tener entre 10 y 500 caracteres")
    String text
) {
    // trim antes de validar: "   hola   " no cuenta como 10 caracteres
    public CommentRequest {
        if (text != null) {
            text = text.trim();
        }
    }
}
