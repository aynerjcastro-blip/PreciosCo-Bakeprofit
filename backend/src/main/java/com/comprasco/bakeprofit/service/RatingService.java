package com.comprasco.bakeprofit.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.dto.RatingResponse;
import com.comprasco.bakeprofit.dto.RatingSummaryResponse;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.Rating;
import com.comprasco.bakeprofit.entity.Store;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.entity.Role;
import com.comprasco.bakeprofit.exception.ForbiddenRoleException;
import com.comprasco.bakeprofit.exception.InvalidRatingFilterException;
import com.comprasco.bakeprofit.exception.PriceNotFoundException;
import com.comprasco.bakeprofit.exception.RatingAlreadyExistsException;
import com.comprasco.bakeprofit.exception.RatingNotAllowedException;
import com.comprasco.bakeprofit.repository.PriceRepository;
import com.comprasco.bakeprofit.repository.RatingRepository;
import com.comprasco.bakeprofit.repository.RatingStats;

@Service
@Transactional(readOnly = true)
public class RatingService {

    private final RatingRepository ratingRepository;
    private final PriceRepository priceRepository;
    private final ProductService productService;
    private final StoreService storeService;

    public RatingService(RatingRepository ratingRepository,
                         PriceRepository priceRepository,
                         ProductService productService,
                         StoreService storeService) {
        this.ratingRepository = ratingRepository;
        this.priceRepository = priceRepository;
        this.productService = productService;
        this.storeService = storeService;
    }

    /**
     * Registra la calificación de un producto en una tienda (RF13, RF16, RF18).
     * Orden de validaciones: existencia (404) -> activos (409) -> precio del par (404) -> duplicado (409).
     */
    @Transactional
    public RatingResponse create(User user, Long productId, Long storeId, Integer score) {
        if (user.getRole() != Role.USER) {
            throw new ForbiddenRoleException("Solo los usuarios pueden calificar.");
        }

        Product product = productService.findById(productId);   // 404 si no existe
        Store store = storeService.findById(storeId);           // 404 si no existe

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new RatingNotAllowedException(
                    "No se puede calificar el producto '" + product.getName() + "' porque está inactivo.");
        }
        if (!Boolean.TRUE.equals(store.getActive())) {
            throw new RatingNotAllowedException(
                    "No se puede calificar en la tienda '" + store.getName() + "' porque está inactiva.");
        }
        if (!priceRepository.existsByProductIdAndStoreId(productId, storeId)) {
            throw new PriceNotFoundException(
                    "El producto '" + product.getName() + "' no tiene precio registrado en la tienda '"
                    + store.getName() + "'.");
        }
        if (ratingRepository.existsByUserIdAndProductIdAndStoreId(user.getId(), productId, storeId)) {
            throw new RatingAlreadyExistsException(product.getName(), store.getName());
        }

        try {
            // IDENTITY inserta de inmediato; saveAndFlush deja explícito que el error sale aquí
            Rating saved = ratingRepository.saveAndFlush(Rating.of(user, product, store, score));
            return RatingResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones simultáneas pasaron la pre-comprobación: gana la restricción única de la BD
            throw new RatingAlreadyExistsException(product.getName(), store.getName());
        }
    }

    /**
     * Resumen (RF15). Con productId y storeId: producto en esa tienda.
     * Solo productId: producto en todas las tiendas. Solo storeId: derivado de la tienda.
     */
    public RatingSummaryResponse getSummary(Long productId, Long storeId) {
        if (productId == null && storeId == null) {
            throw new InvalidRatingFilterException("Debes indicar productId, storeId o ambos.");
        }
        if (productId != null) productService.findById(productId);
        if (storeId != null) storeService.findById(storeId);

        RatingStats stats;
        if (productId != null && storeId != null) {
            stats = ratingRepository.statsByProductAndStore(productId, storeId);
        } else if (productId != null) {
            stats = ratingRepository.statsByProduct(productId);
        } else {
            stats = ratingRepository.statsByStore(storeId);
        }
        return RatingSummaryResponse.of(stats.getAverage(), stats.getTotal());
    }

    /** Calificación del usuario para ese par; vacío si aún no calificó (el controller responde 204). */
    public Optional<RatingResponse> findMine(User user, Long productId, Long storeId) {
        productService.findById(productId);
        storeService.findById(storeId);
        return ratingRepository
                .findByUserIdAndProductIdAndStoreId(user.getId(), productId, storeId)
                .map(RatingResponse::from);
    }
}
