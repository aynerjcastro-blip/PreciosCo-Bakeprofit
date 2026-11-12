package com.comprasco.bakeprofit.auth.security;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.exception.UserNotFoundException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


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