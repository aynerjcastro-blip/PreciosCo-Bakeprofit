package com.comprasco.bakeprofit.repository;

import java.time.LocalDateTime;

/**
 * Posición de una oferta PENDING en el orden de procesamiento (scraped_at, id).
 * El procesador pide la página siguiente a partir de la última clave vista.
 */
public record PendingOfferKey(Long id, LocalDateTime scrapedAt) {
}
