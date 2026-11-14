package com.comprasco.bakeprofit.catalog.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de {@code GET /api/landing/stats}.
 *
 * <p>
 * Contiene, en una sola petición, los indicadores globales y por sección
 * necesarios para renderizar la landing page.
 * </p>
 *
 * @param totalProducts cantidad de productos activos que tienen al menos un
 *                      precio en una tienda activa
 * @param totalStores   cantidad de tiendas activas
 * @param lastUpdate    fecha y hora de la última confirmación de precio
 *                      realizada por una tienda activa; {@code null} si
 *                      todavía no existe ningún precio registrado
 * @param sections      estadísticas agrupadas por categoría raíz
 */
public record LandingStatsResponse(
        long totalProducts,
        long totalStores,
        LocalDateTime lastUpdate,
        List<SectionStats> sections) {

    /**
     * Estadísticas de una sección de la landing.
     *
     * <p>
     * Una sección corresponde a una categoría raíz, por ejemplo
     * “Supermercado” o “Droguería”.
     * </p>
     *
     * @param id           identificador de la categoría raíz
     * @param name         nombre visible de la categoría raíz
     * @param productCount cantidad de productos distintos y activos con precio
     *                     en tiendas activas de esta categoría
     * @param storeCount   cantidad de tiendas activas pertenecientes a la categoría
     */
    public record SectionStats(
            Long id,
            String name,
            long productCount,
            long storeCount) {
    }
}