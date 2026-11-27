package com.comprasco.bakeprofit.subscription.config;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.premium")
public record PremiumCheckoutProperties(
    BigDecimal price,
    String currency,
    URI successUrl,
    URI failureUrl,
    URI pendingUrl
) {
}
