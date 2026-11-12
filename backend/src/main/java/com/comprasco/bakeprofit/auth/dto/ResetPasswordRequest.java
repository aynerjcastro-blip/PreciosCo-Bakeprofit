package com.comprasco.bakeprofit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank (message = "El token es obligatorio")
    String token,
    @NotBlank (message = "La contraseña es obligatoria")
    @Size(min = 8, message="La contraseña debe contener al menos 8 caracteres")
    String newPassword
){}
