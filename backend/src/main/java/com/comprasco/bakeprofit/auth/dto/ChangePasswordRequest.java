package com.comprasco.bakeprofit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/users/me/password}.
 * <p>
 * La confirmación de la nueva contraseña se valida en el frontend: aquí solo
 * llegan la actual (para comprobar que quien pide el cambio la conoce) y la nueva.
 *
 * @param currentPassword contraseña vigente de la cuenta
 * @param newPassword     contraseña nueva (mínimo 8, igual que en el restablecimiento; máximo 72 porque BCrypt no lee más)
 */
public record ChangePasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
        String newPassword
) {
}