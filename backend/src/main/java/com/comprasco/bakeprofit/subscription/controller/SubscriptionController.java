package com.comprasco.bakeprofit.subscription.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.auth.entity.User;
import com.comprasco.bakeprofit.auth.security.CurrentUser;
import com.comprasco.bakeprofit.core.response.ErrorResponse;
import com.comprasco.bakeprofit.subscription.dto.CheckoutIntentResponse;
import com.comprasco.bakeprofit.subscription.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final PaymentService paymentService;

    public SubscriptionController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Iniciar el checkout de Premium",
        description = "Requiere el rol USER. Crea un intento de pago y devuelve la URL de Checkout Pro. "
            + "La acreditación se confirma mediante el webhook de Mercado Pago.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Intento creado",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = CheckoutIntentResponse.class)))
    @ApiResponse(responseCode = "401", description = "JWT ausente o inválido, o cuenta inactiva",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol no permitido: solo USER puede iniciar un pago",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La cuenta ya no existe al crear el intento",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La cuenta ya tiene Premium activo",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "429", description = "Límite de solicitudes por IP excedido",
        headers = @Header(name = "Retry-After", description = "Segundos hasta que se restablezca el límite"),
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "No se pudo iniciar el checkout por un fallo interno o de configuración",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "502", description = "Mercado Pago no pudo crear la orden o devolvió datos incompatibles",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutIntentResponse> checkout(@CurrentUser User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createCheckoutIntent(user.getId()));
    }
}
