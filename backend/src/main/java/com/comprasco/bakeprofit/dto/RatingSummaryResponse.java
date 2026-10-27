package com.comprasco.bakeprofit.dto;


public class RatingSummaryResponse {
    private Double averageScore;
    private Long totalRatings;

    public RatingSummaryResponse(Double averageScore, Long totalRatings) {
        this.averageScore = averageScore != null ? Math.round(averageScore * 10.0) / 10.0 : 0.0;
        this.totalRatings = totalRatings != null ? totalRatings : 0L;
    }

    public Double getAverageScore() { return averageScore; }
    public Long getTotalRatings() { return totalRatings; }

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