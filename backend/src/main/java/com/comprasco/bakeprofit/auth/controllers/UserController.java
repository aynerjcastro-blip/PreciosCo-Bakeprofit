package com.comprasco.bakeprofit.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.auth.dto.ProfileResponse;
import com.comprasco.bakeprofit.auth.dto.UpdateDisplayNameRequest;
import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.security.CurrentUser;
import com.comprasco.bakeprofit.auth.service.UserService;

import jakarta.validation.Valid;

/**
 * Endpoints del perfil del usuario autenticado.
 * <p>
 * Todas las rutas operan sobre el usuario dueño del token JWT de la petición,
 * por lo que no reciben ningún id en la URL: un usuario nunca puede ver ni
 * modificar el perfil de otro.
 */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Devuelve el perfil del usuario autenticado.
     *
     * @param user usuario resuelto desde el token JWT (anotación
     *             {@link CurrentUser})
     * @return 200 con los datos públicos del perfil
     */
    @GetMapping
    public ResponseEntity<ProfileResponse> getMyProfile(@CurrentUser User user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    /**
     * Actualiza el nombre a mostrar del usuario autenticado.
     * <p>
     * Solo permite cambiar el nombre; el email y el resto de campos no se tocan.
     *
     * @param request nuevo nombre (validado: obligatorio, máximo 100 caracteres)
     * @param user    usuario resuelto desde el token JWT
     * @return 200 con el perfil actualizado, con la misma forma que el GET
     */
    @PutMapping
    public ResponseEntity<ProfileResponse> updateMyDisplayName(
            @Valid @RequestBody UpdateDisplayNameRequest request,
            @CurrentUser User user) {
        return ResponseEntity.ok(userService.updateDisplayName(user.getId(), request));
    }
}