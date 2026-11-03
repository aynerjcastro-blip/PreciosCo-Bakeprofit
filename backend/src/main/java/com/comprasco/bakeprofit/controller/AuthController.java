package com.comprasco.bakeprofit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.comprasco.bakeprofit.dto.AuthResponse;
import com.comprasco.bakeprofit.dto.LoginRequest;
import com.comprasco.bakeprofit.dto.RegisterRequest;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.security.CurrentUser;
import com.comprasco.bakeprofit.service.UserService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;

    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // delega al service, devuelve JSON con el usuario creado (o el JWT directo)
        return ResponseEntity.ok(userService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // delega al service, devuelve JSON con el JWT
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    /**
     * Obtiene los datos del usuario actualmente autenticado.
     * Demuestra la inyeccion automatica de la entidad User mediante @CurrentUser
     * (RN-10).
     *
     * @param user entidad de usuario inyectada desde el SecurityContext
     * @return respuesta con los datos del perfil
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getAuthenticatedUser(@CurrentUser User user) {
        AuthResponse response = new AuthResponse(
                null,
                user.getName(),
                user.getEmail(),
                user.getRole());
        return ResponseEntity.ok(response);
    }
}
