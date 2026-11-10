package com.comprasco.bakeprofit.dto;

import com.comprasco.bakeprofit.entity.Role;

public record ProfileResponse(String name, String email, Role role, boolean emailEditable) {
}