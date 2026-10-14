package com.comprasco.bakeprofit.config;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.service.UserService;

/**
 * TEMPORAL. Único punto donde se simula el usuario autenticado, compartido por los controllers.
 * TODO(Ayner): borrar esta clase cuando exista CurrentUser (JwtAuthFilter) y cambiar los controllers.
 * Debe existir un usuario con este email (créalo con POST /api/auth/register).
 */
@Component
public class TemporaryCurrentUser {

    private static final String TEMP_USER_EMAIL = "usuario.prueba@example.com";

    private final UserService userService;

    public TemporaryCurrentUser(UserService userService) {
        this.userService = userService;
    }

    public User get() {
        return userService.findByEmail(TEMP_USER_EMAIL);
    }
}
