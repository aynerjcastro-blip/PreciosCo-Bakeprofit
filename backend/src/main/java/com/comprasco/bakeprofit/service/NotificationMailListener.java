package com.comprasco.bakeprofit.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.comprasco.bakeprofit.event.NotificationCreatedEvent;

/**
 * Envía por correo el aviso de baja de precio (RF28, prioridad DEBERÍA).
 *
 * <p>
 * Solo existe si {@code app.notifications.mail.enabled=true}. Escucha el evento DESPUÉS
 * del commit: si el aviso no se guardó, no sale correo. Cualquier fallo de envío se
 * registra y se descarta: el aviso en la plataforma ya existe, el detector sigue y no se
 * reintenta (así nunca se duplica). El correo no se reenvía si falla.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "app.notifications.mail", name = "enabled", havingValue = "true")
public class NotificationMailListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationMailListener.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JavaMailSender mailSender;
    private final String from;

    public NotificationMailListener(JavaMailSender mailSender,
                                    @Value("${app.notifications.mail.from:}") String from,
                                    @Value("${spring.mail.username:}") String username) {
        this.mailSender = mailSender;
        this.from = from;
        if (username == null || username.isBlank()) {
            log.warn("Correo de avisos activado pero spring.mail.username está vacío: los envíos van a fallar.");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            mailSender.send(buildMessage(event));
            log.info("Correo enviado para el aviso {}", event.notificationId());
        } catch (Exception e) {
            // No se relanza: el aviso ya está guardado y un reintento podría duplicarlo
            log.error("No se pudo enviar el correo del aviso {}", event.notificationId(), e);
        }
    }

    private SimpleMailMessage buildMessage(NotificationCreatedEvent e) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(e.recipientEmail());
        message.setSubject("PreciosCo: bajó el precio de " + oneLine(e.productName()));
        message.setText(buildBody(e));
        return message;
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

    // Evita saltos de línea en el asunto o el cuerpo (inyección de cabeceras)
    private static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("[\\r\\n]+", " ").trim();
    }
}