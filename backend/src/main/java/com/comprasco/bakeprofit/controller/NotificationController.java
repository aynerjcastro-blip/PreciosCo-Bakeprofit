package com.comprasco.bakeprofit.controller;

import com.comprasco.bakeprofit.entity.Notification;
import com.comprasco.bakeprofit.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // Obtener todas las notificaciones del usuario autenticado
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        Long userId = 1L; // ID temporal de prueba (se reemplaza por JWT)
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    // Obtener solo notificaciones no leídas
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        Long userId = 1L;
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    // Marcar una notificación como leída
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));

        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok().build();
    }
}