package com.comprasco.bakeprofit.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.dto.NotificationListResponse;
import com.comprasco.bakeprofit.dto.NotificationResponse;
import com.comprasco.bakeprofit.entity.Follow;
import com.comprasco.bakeprofit.entity.Notification;
import com.comprasco.bakeprofit.entity.Price;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.event.NotificationCreatedEvent;
import com.comprasco.bakeprofit.exception.ForbiddenActionException;
import com.comprasco.bakeprofit.exception.NotificationNotFoundException;
import com.comprasco.bakeprofit.repository.FollowRepository;
import com.comprasco.bakeprofit.repository.NotificationRepository;
import com.comprasco.bakeprofit.repository.PriceRepository;

/**
 * Avisos dentro de la plataforma (RF27, RF53, RF54).
 *
 * <p>
 * {@link #detectForFollow} aplica la regla RN-16 a un seguimiento. Corre en su propia
 * transacción para que un error en un seguimiento no deshaga los avisos de los demás.
 * Es idempotente: el mismo precio nunca genera dos avisos al mismo usuario.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;
    private final PriceRepository priceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(NotificationRepository notificationRepository,
                               FollowRepository followRepository,
                               PriceRepository priceRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.followRepository = followRepository;
        this.priceRepository = priceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Revisa, tienda por tienda, el último precio del producto seguido frente al anterior
     * y crea un aviso si cumple la regla de alerta (RN-15 y RN-16):
     * <ul>
     *   <li>hubo una reducción (último &lt; anterior): sin precio anterior no hay aviso;</li>
     *   <li>el último precio se registró después de {@code alertActiveSince};</li>
     *   <li>si hay precio objetivo, el último precio es menor o igual a él;</li>
     *   <li>la tienda está activa y todavía no existe aviso para ese precio.</li>
     * </ul>
     *
     * @return cantidad de avisos creados
     */
    @Transactional
    public int detectForFollow(Long followId) {
        Follow follow = followRepository.findById(followId).orElse(null);
        if (follow == null || !Boolean.TRUE.equals(follow.getNotificationActive())) {
            return 0;
        }
        User user = follow.getUser();
        if (!Boolean.TRUE.equals(user.getActive())
                || !Boolean.TRUE.equals(follow.getProduct().getActive())) {
            return 0;
        }

        int created = 0;
        Long productId = follow.getProduct().getId();
        for (Long storeId : priceRepository.findStoreIdsByProductId(productId)) {
            List<Price> lastTwo = priceRepository
                    .findTop2ByProductIdAndStoreIdOrderByRegistrationDateDescIdDesc(productId, storeId);
            if (lastTwo.size() < 2) {
                continue; // sin precio anterior no hay baja que avisar
            }
            Price latest = lastTwo.get(0);
            Price previous = lastTwo.get(1);

            if (!Boolean.TRUE.equals(latest.getStore().getActive())
                    || !isAlert(follow, latest, previous)
                    || notificationRepository.existsByUserIdAndPriceId(user.getId(), latest.getId())) {
                continue;
            }

            // Sin try/catch a propósito: una violación de la única dentro de la transacción
            // la deja marcada para rollback. El detector registra el error y la siguiente
            // corrida encuentra el aviso ya creado.
            Notification notification = notificationRepository.saveAndFlush(Notification.of(
                    user, follow.getProduct(), latest.getStore(), latest,
                    previous.getValue(), latest.getValue()));
                        eventPublisher.publishEvent(NotificationCreatedEvent.from(notification));
            created++;
        }
        return created;
    }

    /** Mis avisos más recientes con la cantidad de no leídos (RF53). */
    public NotificationListResponse list(User user) {
        requireUserRole(user);
        long unread = notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
        List<NotificationResponse> items = notificationRepository
                .findTop50ByUserIdOrderByCreatedAtDescIdDesc(user.getId()).stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationListResponse(unread, items);
    }

    /** Marca un aviso como leído (RF54). Repetirlo no hace nada. */
    @Transactional
    public void markRead(User user, Long notificationId) {
        requireUserRole(user);
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.markRead();
    }

    private boolean isAlert(Follow follow, Price latest, Price previous) {
        BigDecimal target = follow.getTargetPrice();
        boolean reduced = latest.getValue().compareTo(previous.getValue()) < 0;
        boolean fresh = latest.getRegistrationDate().isAfter(follow.getAlertActiveSince());
        boolean reachesTarget = target == null || latest.getValue().compareTo(target) <= 0;
        return reduced && fresh && reachesTarget;
    }

    private void requireUserRole(User user) {
        if (user.getRole() != Role.USER) {
            throw new ForbiddenActionException("Solo los usuarios tienen avisos.");
        }
    }
}