package com.comprasco.bakeprofit.price.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.follow.repository.FollowRepository;
import com.comprasco.bakeprofit.notification.event.PriceRegisteredEvent;
import com.comprasco.bakeprofit.notification.service.NotificationService;

/**
 * Avisa las bajas de precio apenas el procesador de staging registra un precio nuevo (HT-11).
 *
 * <p>
 * Escucha {@link PriceRegisteredEvent}, que el procesador publica después del commit de
 * cada oferta y fuera de toda transacción. Por cada seguimiento activo del producto llama a
 * {@link NotificationService#detectForFollow(Long)}, que aplica RN-15 y RN-16 (baja real,
 * precio objetivo, tienda activa) en su propia transacción y nunca repite un aviso. Así hay
 * una sola conexión a la vez y un error en un seguimiento no detiene a los demás ni cambia
 * el estado de la oferta.
 * </p>
 *
 * <p>
 * Un cambio de tamaño nunca llega aquí como baja (SCRUM-267): un producto no cambia de
 * tamaño (RN-02) y otro tamaño es otro producto, con su propio historial.
 * </p>
 *
 * <p>
 * Va en el mismo hilo, sin {@code @Async} (ver {@link PriceRegisteredEvent}). Si la
 * aplicación se detiene antes de publicar el evento, {@link PriceDropDetector} encuentra
 * esa baja en su próxima pasada.
 * </p>
 */
@Component
public class PriceDropListener {

    private static final Logger log = LoggerFactory.getLogger(PriceDropListener.class);

    private final FollowRepository followRepository;
    private final NotificationService notificationService;

    public PriceDropListener(FollowRepository followRepository, NotificationService notificationService) {
        this.followRepository = followRepository;
        this.notificationService = notificationService;
    }

    @EventListener
    public void onPriceRegistered(PriceRegisteredEvent event) {
        int created = 0;
        for (Long followId : followRepository.findActiveIdsByProductId(event.productId())) {
            try {
                created += notificationService.detectForFollow(followId);
            } catch (RuntimeException e) {
                log.error("Aviso de baja: falló el seguimiento {} del precio {}", followId, event.priceId(), e);
            }
        }
        if (created > 0) {
            log.info("Aviso de baja: {} aviso(s) por el precio {}", created, event.priceId());
        }
    }
}
