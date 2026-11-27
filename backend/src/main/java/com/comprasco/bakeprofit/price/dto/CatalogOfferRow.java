package com.comprasco.bakeprofit.price.dto;

import java.math.BigDecimal;

/**
 * Fila plana producto + precio vigente en una tienda, para armar el contexto
 * del asistente de IA en una sola consulta (sin cargar entidades ni sus
 * relaciones EAGER).
 */
public record CatalogOfferRow(
        Long productId,
        String productName,
        String unit,
        String brandName,
        String storeName,
        BigDecimal value,
        String productUrl,
        Boolean requiresPrescription) {

    /** Marcado en la BD como "requiere fórmula médica" (HU-39). */
    public boolean flaggedAsPrescription() {
        return Boolean.TRUE.equals(requiresPrescription);
    }
}
