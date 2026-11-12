package com.comprasco.bakeprofit.price.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceCompareResponse(
    String productName,
    Long storeId,
    String storeName,
    BigDecimal value,
    String source,
    LocalDateTime registrationDate,
    String productUrl,
    String imageUrl,
    // Última vez que el scraper vio este mismo precio; registrationDate es cuándo empezó
    LocalDateTime lastSeenAt
) {}