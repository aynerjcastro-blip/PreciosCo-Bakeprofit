package com.comprasco.bakeprofit.subscription.exception;

public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException() {
        super("Firma de Mercado Pago ausente o inválida, o ID firmado inconsistente.");
    }
}
