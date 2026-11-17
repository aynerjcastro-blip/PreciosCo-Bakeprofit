package com.comprasco.bakeprofit.staging.identity;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("preciosco.staging.secondary-identity")
public record SecondaryIdentityProperties(Set<String> stores, double minimumScore,
                                        double minimumMargin, int candidateLimit) {
    public SecondaryIdentityProperties {
        stores = stores == null ? Set.of() : Set.copyOf(stores);
        if (!Double.isFinite(minimumScore) || minimumScore <= 0 || minimumScore > 1
                || !Double.isFinite(minimumMargin) || minimumMargin <= 0 || minimumMargin > 1
                || candidateLimit < 2) {
            throw new IllegalArgumentException("Configuración de identidad secundaria inválida");
        }
    }
}
