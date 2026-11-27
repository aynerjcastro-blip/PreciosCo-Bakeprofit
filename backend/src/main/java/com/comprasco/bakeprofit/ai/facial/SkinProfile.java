package com.comprasco.bakeprofit.ai.facial;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que el usuario pidió, extraído de su mensaje (HU-40).
 *
 * @param facialRequest false si el mensaje no es sobre cuidado facial
 * @param skinType      grasa, seca, mixta, normal o sensible; vacío si no lo dijo
 * @param needs         necesidades normalizadas (acne, manchas, arrugas...)
 * @param budget        presupuesto máximo POR PRODUCTO en pesos; null si no lo indicó
 * @param productTypes  tipos de producto a recomendar, en orden de rutina
 */
public record SkinProfile(boolean facialRequest, String skinType, List<String> needs, BigDecimal budget,
        List<FacialProductType> productTypes) {

    public boolean hasSkinType() {
        return skinType != null && !skinType.isBlank();
    }
}
