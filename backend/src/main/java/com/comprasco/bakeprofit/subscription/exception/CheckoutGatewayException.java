package com.comprasco.bakeprofit.subscription.exception;

public class CheckoutGatewayException extends RuntimeException {

    public CheckoutGatewayException() {
        super("No se pudo iniciar el pago con Mercado Pago. Intenta de nuevo.");
    }
}
