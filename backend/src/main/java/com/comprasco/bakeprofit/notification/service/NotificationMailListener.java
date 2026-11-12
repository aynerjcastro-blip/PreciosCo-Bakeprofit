package com.comprasco.bakeprofit.notification.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.comprasco.bakeprofit.notification.event.NotificationCreatedEvent;

/**
 * Escucha eventos de creación de notificaciones para armar los avisos de baja
 * de precio y delegar su envío al servicio de correos.
 */
@Component
@ConditionalOnProperty(prefix = "app.notifications.mail", name = "enabled", havingValue = "true")
public class NotificationMailListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationMailListener.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailService emailService;

    /**
     * Inyecta el servicio de correos genérico en lugar del JavaMailSender.
     */
    public NotificationMailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Se ejecuta solo DESPUÉS de un commit exitoso (TransactionPhase.AFTER_COMMIT).
     * Arma el asunto y el cuerpo del mensaje, y delega el envío al EmailService.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            String subject = "PreciosCo: bajó el precio de " + oneLine(event.productName());
            String body = buildBody(event);
            
            emailService.sendEmail(event.recipientEmail(), subject, body);
            
            log.info("Correo enviado para el aviso {}", event.notificationId());
        } catch (Exception e) {
            // No relanzamos para evitar revertir transacciones previas
            log.error("No se pudo enviar el correo del aviso {}", event.notificationId(), e);
        }
    }

    // Mismos datos del aviso en la plataforma: producto, tienda, precio anterior, nuevo y fecha
    private String buildBody(NotificationCreatedEvent e) {
        return "Hola " + oneLine(e.recipientName()) + ",\n\n"
                + "Bajó el precio de un producto que sigues:\n\n"
                + "Producto: " + oneLine(e.productName()) + "\n"
                + "Tienda: " + oneLine(e.storeName()) + "\n"
                + "Precio anterior: " + money(e.previousPrice()) + "\n"
                + "Precio nuevo: " + money(e.newPrice()) + "\n"
                + "Fecha: " + e.createdAt().format(DATE_FORMAT) + "\n\n"
                + "Puedes pausar este aviso desde la página Seguidos de PreciosCo.\n";
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.of("es", "CO"));
        format.setMaximumFractionDigits(2);
        return "$" + format.format(value);
    }

    // Evita saltos de línea en el asunto o el cuerpo (prevención de inyección de cabeceras)
    private static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n]+", " ").trim();
    }
}