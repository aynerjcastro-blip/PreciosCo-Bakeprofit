package com.comprasco.bakeprofit.auth.dto;

import com.comprasco.bakeprofit.auth.entity.Role;

public record ProfileResponse(String name, String email, Role role, boolean emailEditable) {
}