package com.comprasco.bakeprofit.catalog.service;

import com.comprasco.bakeprofit.catalog.dto.LandingStatsResponse;
import com.comprasco.bakeprofit.catalog.dto.LandingStatsResponse.SectionStats;
import com.comprasco.bakeprofit.catalog.exception.LandingStatsRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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

    private static final String CACHE_KEY = "stats";

    private final LandingStatsRepository repository;
    private final Clock clock;
    /**
     * Las cifras de la portada cambian solo cuando corre el procesador de precios: se
     * guardan unos minutos para no repetir 4 consultas contra la BD remota (~0,9 s) en cada
     * visita.
     */
    private final Cache<String, LandingStatsResponse> cache;

    /**
     * Crea el servicio con el repositorio encargado de ejecutar las
     * consultas de estadísticas.
     *
     * @param repository repositorio de estadísticas de la landing
     */
    public LandingStatsService(LandingStatsRepository repository, Clock clock,
            @Value("${app.landing.stats-cache-ttl:PT5M}") Duration cacheTtl) {
        this.repository = repository;
        this.clock = clock;
        this.cache = Caffeine.newBuilder().expireAfterWrite(cacheTtl).maximumSize(1).build();
    }

    /**
     * Obtiene las estadísticas agregadas para la landing page (en caché unos minutos).
     *
     * @return respuesta con contadores globales, fecha de última
     *         actualización y estadísticas por sección
     */
    public LandingStatsResponse getStats() {
        return cache.get(CACHE_KEY, key -> computeStats());
    }

    private LandingStatsResponse computeStats() {
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