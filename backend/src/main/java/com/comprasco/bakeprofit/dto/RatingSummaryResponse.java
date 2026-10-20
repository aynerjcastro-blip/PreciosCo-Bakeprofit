package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.swagger.v3.oas.annotations.media.Schema;

public record RatingSummaryResponse(
    @Schema(description = "Promedio con un decimal; null si no hay calificaciones", example = "4.0")
    BigDecimal averageScore,
    
    @Schema(description = "Cantidad de calificaciones", example = "2")
    long ratingCount
) {
    public static RatingSummaryResponse of(Double average, long count) {
        BigDecimal avg = average == null
            ? null
            : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        return new RatingSummaryResponse(avg, count);
    }
}