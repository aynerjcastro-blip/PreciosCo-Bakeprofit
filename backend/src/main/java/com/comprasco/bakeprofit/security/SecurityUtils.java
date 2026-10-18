package com.comprasco.bakeprofit.security;

import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.UserNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Helper utilitario para recuperar el usuario autenticado desde cualquier capa
 * del backend.
 */
public class SecurityUtils {

    /**
     * Obtiene la entidad User cargada en el contexto de seguridad.
     *
     * @return entidad User del usuario autenticado
     * @throws UserNotFoundException si no existe autenticacion valida
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new UserNotFoundException("No se encontro un usuario autenticado en el contexto actual.");
    }

}