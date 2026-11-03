package com.comprasco.bakeprofit.repository;

/**
 * Proyección de las consultas de resumen de calificaciones.
 * Los nombres de los getters deben coincidir con los alias (average, total) de las @Query.
 */
public interface RatingStats {
    Double getAverage();   // null cuando no hay calificaciones
    Long getTotal();       // 0 cuando no hay calificaciones
}
