package com.comprasco.bakeprofit.ai.exception;

/**
 * Error al comunicarse con el proveedor de IA (Gemini).
 *
 * <p>
 * El mensaje es seguro para mostrarse al usuario; el detalle técnico
 * (cuerpo de la respuesta de Gemini) solo se registra en el log del servidor.
 * </p>
 */
public class AiProviderException extends RuntimeException {

    private final int providerStatus;

    public AiProviderException(int providerStatus, String userMessage) {
        super(userMessage);
        this.providerStatus = providerStatus;
    }

    public AiProviderException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.providerStatus = 0;
    }

    /** Código HTTP devuelto por Gemini (0 si no hubo respuesta). */
    public int getProviderStatus() {
        return providerStatus;
    }
}
