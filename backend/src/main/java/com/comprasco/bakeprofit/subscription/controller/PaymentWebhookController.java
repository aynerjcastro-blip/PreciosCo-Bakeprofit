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

import com.comprasco.bakeprofit.core.response.ErrorResponse;
import com.comprasco.bakeprofit.subscription.service.OrderWebhookService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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

    /**
     * Mercado Pago firma el ID de la URL, por lo que el ID del cuerpo debe coincidir antes de
     * consultar la orden. El ID firmado permite omitir la consulta externa cuando ya se registró
     * un vencimiento, una cancelación o un reembolso total. Las órdenes acreditadas se consultan
     * de nuevo porque aún pueden reembolsarse.
     *
     * Cuando se registra un rechazo correctamente, {@code 200} confirma la recepción del aviso
     * para que el resultado del pago no provoque otro intento de entrega.
     */
    @Operation(summary = "Recibir un webhook de Mercado Pago",
        description = "Ruta pública. Mercado Pago firma la entrega mediante HMAC y no envía JWT. "
            + "La confirmación de entrega es independiente del resultado del pago.",
        parameters = {
            @Parameter(name = "data.id", in = ParameterIn.QUERY, required = true,
                description = "ID de la orden incluido en la firma para el tópico order"),
            @Parameter(name = "x-signature", in = ParameterIn.HEADER, required = true,
                description = "Firma HMAC de la entrega, con los componentes ts y v1"),
            @Parameter(name = "x-request-id", in = ParameterIn.HEADER, required = true,
                description = "Identificador de la entrega incluido en el manifiesto de la firma")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Notificación JSON de Orders. El ID del cuerpo debe coincidir con el ID firmado de la URL.",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                examples = @ExampleObject(value = """
                    {"type":"order","action":"order.processed","data":{"id":"ORDTST01M4V4VSXCTN8ZN1FJ02KKMFGE"}}
                    """))))
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Entrega confirmada, incluidos pagos rechazados y avisos ignorados",
        content = @Content)
    @ApiResponse(responseCode = "400", description = "JSON ilegible o ID de orden con formato inválido", content = @Content)
    @ApiResponse(responseCode = "401", description = "Firma ausente o inválida, o ID firmado inconsistente",
        content = @Content)
    @ApiResponse(responseCode = "429", description = "Límite de solicitudes por IP excedido",
        headers = @Header(name = "Retry-After", description = "Segundos hasta que se restablezca el límite"),
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Fallo interno o configuración incompleta del webhook",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "No se pudo consultar la orden en Mercado Pago", content = @Content)
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
