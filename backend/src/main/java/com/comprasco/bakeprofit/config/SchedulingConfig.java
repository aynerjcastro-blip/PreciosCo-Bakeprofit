package com.comprasco.bakeprofit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas {@code @Scheduled} (hoy, el detector de bajas de precio).
 * Va en su propia clase para no tocar {@code BakeprofitApplication}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}