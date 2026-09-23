package com.comprasco.bakeprofit.dto;

import com.comprasco.bakeprofit.entity.Role;

public record AuthResponse(
    String token,
    String name,
    String email,
    Role role
)
{}
