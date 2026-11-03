package com.comprasco.bakeprofit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.repository.FollowRepository;

/**
 * Tarea programada que revisa las bajas de precio de los productos seguidos.
 *
 * <p>
 * El scraper inserta directo en la BD desde Python, así que no hay un punto del
 * código Java donde enganchar el aviso: el detector lee la tabla {@code prices}
 * cada cierto tiempo. La frecuencia sale de {@code app.notifications.check-interval}
 * (duración ISO-8601, {@code PT24H} por defecto). Cada seguimiento se procesa en su
 * propia transacción y un fallo en uno no detiene a los demás.
 * </p>
 */
@Component
public class PriceDropDetector {

    private static final Logger log = LoggerFactory.getLogger(PriceDropDetector.class);

    private final FollowRepository followRepository;
    private final NotificationService notificationService;

    public PriceDropDetector(FollowRepository followRepository, NotificationService notificationService) {
        this.followRepository = followRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(
        fixedDelayString = "${app.notifications.check-interval:PT24H}",
        initialDelayString = "${app.notifications.initial-delay:PT1M}")
    public void run() {
        int created = detect();
        log.info("Detector de bajas: {} aviso(s) nuevo(s)", created);
    }

    /** Una pasada completa. Público para poder invocarlo desde pruebas. */
    public int detect() {
        int created = 0;
        for (Long followId : followRepository.findActiveIds()) {
            try {
                created += notificationService.detectForFollow(followId);
            } catch (RuntimeException e) {
                log.error("Detector de bajas: falló el seguimiento {}", followId, e);
            }
        }
        return created;
    }
}