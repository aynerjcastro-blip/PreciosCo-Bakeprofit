package com.comprasco.bakeprofit.subscription.exception;

public class PremiumAlreadyActiveException extends RuntimeException {

    public PremiumAlreadyActiveException() {
        super("Tu cuenta ya tiene Premium activo.");
    }
}
