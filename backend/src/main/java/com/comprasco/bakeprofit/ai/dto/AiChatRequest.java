package com.comprasco.bakeprofit.ai.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Petición de chat del frontend.
 *
 * @param section slug de la sección: "supermercados", "droguerias" o
 *                "cuidado-facial"
 * @param message texto que escribió el usuario
 * @param history turnos anteriores de la conversación (el frontend los guarda
 *                en memoria)
 */
public record AiChatRequest(

        @NotBlank(message = "La sección es obligatoria") String section,

        @NotBlank(message = "El mensaje no puede estar vacío") @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres") String message,

        List<Turn> history) {

    /** Un turno previo de la conversación */
    public record Turn(
            @NotNull String role, // "user" o "assistant"
            @NotNull String content) {
    }
}