package com.comprasco.bakeprofit.catalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.catalog.event.ProductPrescriptionChangedEvent;
import com.comprasco.bakeprofit.catalog.exception.ProductNotFoundException;
import com.comprasco.bakeprofit.catalog.repository.ProductRepository;

/**
 * Marca de "requiere fórmula médica" de los productos (HU-39, RN-33, DP-56).
 *
 * <p>
 * Es la fuente que cubre lo que la lista de principios activos no detecta por
 * el nombre, por ejemplo un medicamento de fórmula vendido con nombre
 * comercial. Al cambiarla se publica un evento para que el asistente de IA
 * descarte su catálogo en caché.
 * </p>
 */
@Service
public class ProductPrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(ProductPrescriptionService.class);

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProductPrescriptionService(ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @throws ProductNotFoundException si el producto no existe
     */
    @Transactional
    public void setRequiresPrescription(Long productId, boolean required) {
        if (productRepository.updateRequiresPrescription(productId, required) == 0) {
            throw new ProductNotFoundException("Producto no encontrado con id: " + productId);
        }
        log.info("Producto {} marcado como {} (RN-33)", productId,
                required ? "de fórmula médica" : "de venta libre");
        eventPublisher.publishEvent(new ProductPrescriptionChangedEvent(productId, required));
    }
}
