package com.comprasco.bakeprofit.staging.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.catalog.entity.Category;
import com.comprasco.bakeprofit.catalog.repository.CategoryRepository;
import com.comprasco.bakeprofit.store.rule.StoreDomainPolicy;

/**
 * Tarea programada del procesador de staging. Solo existe con
 * {@code preciosco.staging.enabled=true} (variable STAGING_ENABLED), así que las pruebas
 * y los equipos que no la activan nunca procesan datos de la base.
 *
 * <p>
 * Usa {@code fixedDelay}: la siguiente corrida empieza cuando termina la anterior, así
 * que nunca se solapan. El planificador de Spring tiene un solo hilo por defecto, de modo
 * que tampoco coincide con el detector de bajas; no se debe subir
 * {@code spring.task.scheduling.pool.size} sin revisar el pool de conexiones.
 * </p>
 */
@Component
@ConditionalOnProperty(prefix = "preciosco.staging", name = "enabled", havingValue = "true")
public class StagingScheduler {

    private static final Logger log = LoggerFactory.getLogger(StagingScheduler.class);

    private final StagingProcessor processor;
    private final CategoryRepository categoryRepository;
    private final StoreDomainPolicy domainPolicy;

    public StagingScheduler(StagingProcessor processor, CategoryRepository categoryRepository,
                            StoreDomainPolicy domainPolicy) {
        this.processor = processor;
        this.categoryRepository = categoryRepository;
        this.domainPolicy = domainPolicy;
    }

    @Scheduled(
        fixedDelayString = "${preciosco.staging.run-interval}",
        initialDelayString = "${preciosco.staging.initial-delay}")
    public void run() {
        processor.runOnce();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warnAboutUnmatchedDomains() {
        List<String> roots = categoryRepository.findByParentIsNull().stream()
                .map(Category::getName)
                .toList();
        List<String> unmatched = domainPolicy.unmatchedDomains(roots);
        if (!unmatched.isEmpty()) {
            log.warn("Procesador de staging: los dominios {} no coinciden con ninguna categoría raíz {}; "
                    + "sus reglas no se aplicarán", unmatched, roots);
        }
    }
}
