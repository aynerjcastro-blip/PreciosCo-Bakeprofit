package com.comprasco.bakeprofit.subscription.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckoutIntentResponse(
    @Schema(description = "UUID del intento local enviado a Mercado Pago como external_reference", format = "uuid")
    String externalReference,
    @Schema(description = "URL de Checkout Pro a la que se redirige al comprador", format = "uri")
    String checkoutUrl
) {
}
