package com.comprasco.bakeprofit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Servicio generico responsable únicamente del envío de correos electrónicos.
 * Aplica el principio de Responsabilidad Única (SRP): encapsula JavaMailSender
 * para que el resto de la aplicación no dependa de librerías de correo.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    /**
     * @param mailSender Proveedor nativo de Spring Boot para envío de correos.
     * @param from       Dirección remitente configurada en las properties (opcional).
     */
    public EmailService(JavaMailSender mailSender, 
                        @Value("${app.notifications.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Construye y envía un correo de texto plano.
     *
     * @param to      Correo del destinatario.
     * @param subject Asunto del correo.
     * @param body    Cuerpo del mensaje en texto plano.
     */
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }
}