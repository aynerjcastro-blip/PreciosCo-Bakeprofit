package com.comprasco.bakeprofit.staging.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra {@link StagingProperties}; la tarea programada se activa aparte, en StagingScheduler. */
@Configuration
@EnableConfigurationProperties(StagingProperties.class)
public class StagingConfig {
}
