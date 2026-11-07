package com.comprasco.bakeprofit.dto;

import java.time.LocalDateTime;

public class PriceDropAlertDto {

    private Long productId;
    private String productName;
    private Double previousPrice;
    private Double currentPrice;
    private LocalDateTime detectedAt;

    public PriceDropAlertDto() {}

    public PriceDropAlertDto(Long productId, String productName, Double previousPrice, Double currentPrice, LocalDateTime detectedAt) {
        this.productId = productId;
        this.productName = productName;
        this.previousPrice = previousPrice;
        this.currentPrice = currentPrice;
        this.detectedAt = detectedAt;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Double getPreviousPrice() { return previousPrice; }
    public void setPreviousPrice(Double previousPrice) { this.previousPrice = previousPrice; }

    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
}