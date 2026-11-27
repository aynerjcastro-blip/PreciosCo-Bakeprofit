package com.comprasco.bakeprofit.subscription.exception;

public class WebhookGatewayException extends RuntimeException {

    public WebhookGatewayException() {
        super("No se pudo verificar la orden en Mercado Pago.");
    }
}
