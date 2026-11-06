package com.comprasco.bakeprofit.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "El token de Google es obligatorio") String credential) {
}