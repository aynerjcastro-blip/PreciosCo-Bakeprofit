package com.comprasco.bakeprofit.catalog.service;

import com.comprasco.bakeprofit.catalog.dto.LandingStatsResponse;
import com.comprasco.bakeprofit.catalog.dto.LandingStatsResponse.SectionStats;
import com.comprasco.bakeprofit.catalog.exception.LandingStatsRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio responsable de calcular y exponer las estadísticas mostradas
 * en la landing page.
 *
 * <p>
 * Consolida, en una única transacción de solo lectura, el número de
 * productos con precio, tiendas activas, última actualización de precios
 * y estadísticas por sección/categoría.
 * </p>
 */
@Service
public class LandingStatsService {

    private final LandingStatsRepository repository;
    private final Clock clock;

    /**
     * Crea el servicio con el repositorio encargado de ejecutar las
     * consultas de estadísticas.
     *
     * @param repository repositorio de estadísticas de la landing
     */
    public LandingStatsService(LandingStatsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Obtiene las estadísticas agregadas para la landing page.
     *
     * <p>
     * Ejecuta las consultas dentro de una transacción de solo lectura,
     * permitiendo que las consultas compartan la misma conexión del pool
     * y evitando escrituras accidentales.
     * </p>
     *
     * @return respuesta con contadores globales, fecha de última
     *         actualización y estadísticas por sección
     */
    @Transactional(readOnly = true)
    public LandingStatsResponse getStats() {
        List<SectionStats> sections = repository.findSectionStats().stream()
                .map(result -> new SectionStats(
                        result.getId(),
                        result.getName(),
                        result.getProductCount(),
                        result.getStoreCount()))
                .toList();

        LocalDateTime lastUpdate = repository.findLastUpdate()
                .orElse(null);

        Instant lastUpdateInstant = lastUpdate == null
                ? null
                : lastUpdate.atZone(clock.getZone()).toInstant();

        return new LandingStatsResponse(
                repository.countProductsWithPrice(),
                repository.countActiveStores(),
                lastUpdateInstant,
                sections);
    }
}