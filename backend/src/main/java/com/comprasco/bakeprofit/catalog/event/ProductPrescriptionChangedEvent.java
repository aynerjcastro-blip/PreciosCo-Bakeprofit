package com.comprasco.bakeprofit.catalog.event;

/**
 * Un administrador marcó o desmarcó un producto como de venta bajo fórmula
 * médica (HU-39). El asistente de IA lo escucha para descartar su catálogo en
 * caché y no seguir enviando un producto recién marcado.
 */
public record ProductPrescriptionChangedEvent(Long productId, boolean requiresPrescription) {
}
