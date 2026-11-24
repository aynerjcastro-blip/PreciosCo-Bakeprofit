package com.comprasco.bakeprofit.subscription.controller;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.comprasco.bakeprofit.subscription.service.OrderWebhookService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final OrderWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public PaymentWebhookController(OrderWebhookService webhookService, ObjectMapper objectMapper,
                                    @Value("${app.mercadopago.webhook-secret:}") String webhookSecret) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/api/payments/webhook/mercadopago")
    public ResponseEntity<Void> receive(HttpServletRequest request, @RequestBody(required = false) String body) {
        var query = UriComponentsBuilder.newInstance().query(request.getQueryString()).build().getQueryParams();
        List<String> dataIds = query.get("data.id");
        String signedId = query.getFirst("data.id");
        if ((dataIds != null && dataIds.size() != 1) || !hasValidSignature(request, signedId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JsonNode notification;
        try {
            notification = objectMapper.readTree(body == null ? "" : body);
        } catch (JacksonException ex) {
            return ResponseEntity.badRequest().build();
        }
        if (notification == null || !"order".equals(notification.path("type").asString(null))) {
            return ResponseEntity.ok().build();
        }

        String bodyId = notification.path("data").path("id").asString(null);
        // El manifiesto de la firma usa el ID de la query. El cuerpo no puede cambiar ese ID.
        if (signedId == null || !signedId.equalsIgnoreCase(bodyId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String orderId = signedId.toUpperCase(Locale.ROOT);
        if (!orderId.matches("ORD[A-Z0-9]{1,47}")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            webhookService.process(orderId);
            return ResponseEntity.ok().build();
        } catch (MPApiException | MPException ex) {
            log.warn("No se pudo verificar la Order {} de Mercado Pago", orderId);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    private boolean hasValidSignature(HttpServletRequest request, String signedId) {
        String signature = request.getHeader("x-signature");
        String requestId = request.getHeader("x-request-id");
        if (!StringUtils.hasText(signature) || !StringUtils.hasText(requestId)
                || !StringUtils.hasText(webhookSecret)) {
            return false;
        }
        String manifestId = signedId == null ? null : signedId.toLowerCase(Locale.ROOT);
        try {
            WebhookSignatureValidator.validate(signature, requestId, manifestId, webhookSecret);
            return true;
        } catch (MPInvalidWebhookSignatureException ex) {
            return false;
        }
    }
}
