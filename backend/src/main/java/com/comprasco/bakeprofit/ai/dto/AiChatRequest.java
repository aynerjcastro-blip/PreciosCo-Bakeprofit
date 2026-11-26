package com.comprasco.bakeprofit.ai.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Petición de chat del frontend.
 *
 * @param section slug de la sección: "supermercados", "droguerias" o
 *                "cuidado-facial". Obligatorio en {@code /chat}; se ignora en
 *                {@code /supermercados/cotizar}.
 * @param message texto que escribió el usuario
 * @param history turnos anteriores de la conversación (el frontend envía solo
 *                los últimos {@value #MAX_HISTORY_TURNS})
 */
public record AiChatRequest(

        @Pattern(regexp = "supermercados|droguerias|cuidado-facial", message = "Sección desconocida") String section,

        @NotBlank(message = "El mensaje no puede estar vacío") @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres") String message,

        @Size(max = AiChatRequest.MAX_HISTORY_TURNS, message = "El historial no puede superar los "
                + AiChatRequest.MAX_HISTORY_TURNS + " turnos") List<@Valid @NotNull Turn> history) {

    public static final int MAX_HISTORY_TURNS = 20;

    /** Un turno previo de la conversación */
    public record Turn(
            @NotNull @Pattern(regexp = "user|assistant", message = "Rol inválido en el historial") String role,
            @NotBlank @Size(max = 8000, message = "Un turno del historial es demasiado largo") String content) {
    }
}
