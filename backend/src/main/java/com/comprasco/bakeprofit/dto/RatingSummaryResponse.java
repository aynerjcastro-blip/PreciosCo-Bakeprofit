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
}