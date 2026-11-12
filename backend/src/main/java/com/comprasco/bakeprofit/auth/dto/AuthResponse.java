package com.comprasco.bakeprofit.auth.dto;

import com.comprasco.bakeprofit.auth.entity.Role;

public record AuthResponse(
    String token,
    String name,
    String email,
    Role role
)
{}
