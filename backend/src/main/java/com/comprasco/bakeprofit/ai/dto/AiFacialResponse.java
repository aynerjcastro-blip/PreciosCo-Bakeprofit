package com.comprasco.bakeprofit.ai.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Evento final ({@code done}) del stream de cuidado facial (HU-40, RF32). Lleva
 * las opciones que el backend ya filtró por presupuesto, para que el frontend
 * las muestre en una tarjeta.
 *
 * @param queriesUsed  consultas usadas este mes (después de esta)
 * @param queriesLimit límite mensual (0 = ilimitado / PREMIUM)
 * @param plan         "FREE" o "PREMIUM"
 * @param disclaimer   aviso de orientación que el backend agregó al final (RN-17)
 * @param skinType     tipo de piel entendido; null si se preguntó
 * @param needs        necesidades entendidas
 * @param budget       presupuesto por producto; null si no lo indicó
 * @param groups       opciones por tipo de producto, en orden de rutina (vacío si se
 *                     preguntó el tipo de piel)
 */
public record AiFacialResponse(
        int queriesUsed,
        int queriesLimit,
        String plan,
        String disclaimer,
        String skinType,
        List<String> needs,
        BigDecimal budget,
        List<Group> groups) {

    /**
     * @param withinBudget       opciones dentro del presupuesto, la primera es la recomendada
     * @param cheapestOverBudget opción más barata si ninguna cabe en el presupuesto; si no, null
     */
    public record Group(String type, String label, List<Option> withinBudget, Option cheapestOverBudget) {
    }

    public record Option(Long productId, String name, String brand, String store, BigDecimal price, String url) {
    }
}
