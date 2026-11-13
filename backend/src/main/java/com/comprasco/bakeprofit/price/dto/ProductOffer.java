
package com.comprasco.bakeprofit.price.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductOffer(Long productId, Long storeId, String storeName,
      BigDecimal value, LocalDateTime lastSeenAt, String productUrl) {
}

