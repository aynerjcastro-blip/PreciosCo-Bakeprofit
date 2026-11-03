package com.comprasco.bakeprofit.exception;

/**
 * Lanzada cuando el producto no tiene ningún precio registrado en la tienda indicada.
 */
public class PriceNotFoundException extends RuntimeException {
    public PriceNotFoundException(String message) {
        super(message);
    }
}
