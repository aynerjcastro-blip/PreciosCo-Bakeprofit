package com.comprasco.bakeprofit.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.dto.FollowResponse;
import com.comprasco.bakeprofit.entity.Follow;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.exception.FollowAlreadyExistsException;
import com.comprasco.bakeprofit.exception.FollowNotAllowedException;
import com.comprasco.bakeprofit.exception.FollowNotFoundException;
import com.comprasco.bakeprofit.exception.ForbiddenActionException;
import com.comprasco.bakeprofit.repository.FollowRepository;
import com.comprasco.bakeprofit.repository.PriceRepository;

/**
 * Seguimiento de productos (RF23 a RF25). Todas las operaciones son sobre el usuario
 * autenticado: nadie ve ni modifica los seguimientos de otro. Solo el rol USER sigue (RN-07).
 */
@Service
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final PriceRepository priceRepository;
    private final ProductService productService;

    public FollowService(FollowRepository followRepository,
                         PriceRepository priceRepository,
                         ProductService productService) {
        this.followRepository = followRepository;
        this.priceRepository = priceRepository;
        this.productService = productService;
    }

    /**
     * Sigue un producto. Orden: rol (403) -> existencia (404) -> activo (409) -> duplicado (409).
     */
    @Transactional
    public FollowResponse create(User user, Long productId, BigDecimal targetPrice) {
        requireUserRole(user);
        Product product = productService.findById(productId);

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new FollowNotAllowedException(
                    "No se puede seguir el producto '" + product.getName() + "' porque está inactivo.");
        }
        if (followRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new FollowAlreadyExistsException(product.getName());
        }

        try {
            Follow follow = followRepository.saveAndFlush(Follow.of(user, product, targetPrice));
            return toResponse(follow);
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones simultáneas pasaron la pre-comprobación: gana la única de la BD
            throw new FollowAlreadyExistsException(product.getName());
        }
    }

    /** Mis seguimientos, del más reciente al más antiguo. */
    public List<FollowResponse> list(User user) {
        requireUserRole(user);
        return followRepository.findByUserIdOrderByRegistrationDateDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Mi seguimiento de un producto, o vacío si no lo sigo (lo usa el botón Seguir). */
    public Optional<FollowResponse> findMine(User user, Long productId) {
        requireUserRole(user);
        return followRepository.findByUserIdAndProductId(user.getId(), productId)
                .map(this::toResponse);
    }

    /** Cambia el precio objetivo y/o activa o desactiva el aviso. */
    @Transactional
    public FollowResponse update(User user, Long followId, Boolean notificationActive, BigDecimal targetPrice) {
        requireUserRole(user);
        Follow follow = findOwned(user, followId);
        follow.updateAlert(notificationActive, targetPrice);
        return toResponse(follow);
    }

    /** Deja de seguir el producto. */
    @Transactional
    public void delete(User user, Long followId) {
        requireUserRole(user);
        followRepository.delete(findOwned(user, followId));
    }

    private void requireUserRole(User user) {
        if (user.getRole() != Role.USER) {
            throw new ForbiddenActionException("Solo los usuarios pueden seguir productos.");
        }
    }

    // Un seguimiento ajeno responde igual que uno inexistente (404): no se revela que existe
    private Follow findOwned(User user, Long followId) {
        return followRepository.findByIdAndUserId(followId, user.getId())
                .orElseThrow(() -> new FollowNotFoundException(followId));
    }

    private FollowResponse toResponse(Follow follow) {
        BigDecimal lowest = priceRepository.findMinPriceByProduct(follow.getProduct().getId());
        return FollowResponse.from(follow, lowest);
    }
}