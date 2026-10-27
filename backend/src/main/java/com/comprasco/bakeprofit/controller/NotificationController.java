package com.comprasco.bakeprofit.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.dto.NotificationListResponse;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.security.CurrentUser;
import com.comprasco.bakeprofit.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Avisos de baja de precio")
@RestController
@RequestMapping("api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Mis avisos",
            description = "Hasta 50 avisos, del más reciente al más antiguo, con la cantidad de no leídos.")
    @ApiResponse(responseCode = "200", description = "Avisos y cantidad de no leídos")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no tiene avisos")
    @GetMapping
    public ResponseEntity<NotificationListResponse> list(@CurrentUser User user) {
        return ResponseEntity.ok(notificationService.list(user));
    }

    @Operation(summary = "Marcar un aviso como leído",
            description = "Repetirlo también responde 204.")
    @ApiResponse(responseCode = "204", description = "Aviso marcado como leído")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no tiene avisos")
    @ApiResponse(responseCode = "404", description = "El aviso no existe o no es tuyo")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@CurrentUser User user, @PathVariable Long id) {
        notificationService.markRead(user, id);
        return ResponseEntity.noContent().build();
    }
}