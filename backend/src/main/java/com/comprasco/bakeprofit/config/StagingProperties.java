package com.comprasco.bakeprofit.config;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del procesador de staging ({@code preciosco.staging.*}). Los valores
 * viven en {@code application.properties}; aquí solo se validan al arrancar, para que
 * una propiedad mal escrita falle de inmediato y no en la primera corrida.
 *
 * @param enabled                    activa la tarea programada (apagada por defecto y en las pruebas)
 * @param batchSize                  ofertas PENDING que se leen por página
 * @param processedRetention         antigüedad a partir de la cual se borran las PROCESSED
 * @param unclassifiedRetention      antigüedad a partir de la cual se borran las UNCLASSIFIED
 * @param existingBrandsOnlyDomains  dominios (categorías raíz) donde solo se emparejan marcas existentes
 * @param identityPendingDomains     dominios cuyos productos aún no se pueden identificar (sin referencia del fabricante)
 */
@ConfigurationProperties(prefix = "preciosco.staging")
public record StagingProperties(
    boolean enabled,
    int batchSize,
    Duration processedRetention,
    Duration unclassifiedRetention,
    List<String> existingBrandsOnlyDomains,
    List<String> identityPendingDomains
) {

    private static final String POSITIVE_BATCH_MESSAGE =
            "preciosco.staging.batch-size debe ser mayor que cero: %d";
    private static final String POSITIVE_RETENTION_MESSAGE =
            "preciosco.staging.%s debe ser un plazo positivo: %s";
    private static final String MISSING_PROPERTY_MESSAGE = "Falta la propiedad preciosco.staging.%s";

    public StagingProperties {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(POSITIVE_BATCH_MESSAGE.formatted(batchSize));
        }
        requirePositive(processedRetention, "processed-retention");
        requirePositive(unclassifiedRetention, "unclassified-retention");
        existingBrandsOnlyDomains = List.copyOf(Objects.requireNonNullElse(existingBrandsOnlyDomains, List.of()));
        identityPendingDomains = List.copyOf(Objects.requireNonNullElse(identityPendingDomains, List.of()));
    }

    private static void requirePositive(Duration retention, String propertyName) {
        Objects.requireNonNull(retention, MISSING_PROPERTY_MESSAGE.formatted(propertyName));
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException(POSITIVE_RETENTION_MESSAGE.formatted(propertyName, retention));
        }
    }
}
