package com.comprasco.bakeprofit.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceCompareResponse(
    String productName,
    String storeName,
    BigDecimal value,
    LocalDateTime registrationDate
) {}