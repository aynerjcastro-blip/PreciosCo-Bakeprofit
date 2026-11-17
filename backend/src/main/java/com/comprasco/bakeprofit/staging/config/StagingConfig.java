package com.comprasco.bakeprofit.staging.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import com.comprasco.bakeprofit.staging.identity.SecondaryIdentityProperties;

/** Registra {@link StagingProperties}; la tarea programada se activa aparte, en StagingScheduler. */
@Configuration
@EnableConfigurationProperties({StagingProperties.class,
    SecondaryIdentityProperties.class})
public class StagingConfig {
}
