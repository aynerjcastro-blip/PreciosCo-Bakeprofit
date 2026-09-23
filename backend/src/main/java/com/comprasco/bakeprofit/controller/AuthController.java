package com.comprasco.bakeprofit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.comprasco.bakeprofit.dto.AuthResponse;
import com.comprasco.bakeprofit.dto.LoginRequest;
import com.comprasco.bakeprofit.dto.RegisterRequest;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;

    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // delega al service, devuelve JSON con el usuario creado (o el JWT directo)
        return ResponseEntity.ok(userService.register(registerRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        // delega al service, devuelve JSON con el JWT
        return ResponseEntity.ok(userService.login(loginRequest));
    }

}
