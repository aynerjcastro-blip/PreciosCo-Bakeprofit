package com.comprasco.bakeprofit.repository;

import java.math.BigDecimal;

/**
 * Tamaño que trae otra oferta PENDING del mismo EAN, tal como la escribió el scraper
 * (la unidad todavía sin interpretar). Lo usa {@code SizeConflictCheck}.
 */
public interface PendingOfferSize {

    Long getId();

    BigDecimal getNetContent();

    String getUnitBase();

    String getStoreName();
}
