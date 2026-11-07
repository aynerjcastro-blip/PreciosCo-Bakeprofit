package com.comprasco.bakeprofit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.entity.Brand;
import com.comprasco.bakeprofit.repository.BrandRepository;

/**
 * Único creador de marcas. No expone endpoints: la administración de marcas
 * es del panel administrativo (EP-09).
 */
@Service
@Transactional(readOnly = true)
public class BrandService {

    private static final String BRAND_NOT_FOUND_AFTER_INSERT_MESSAGE =
            "La marca '%s' no se encontró después de registrarla.";

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    /**
     * Devuelve la marca con ese nombre sin distinguir mayúsculas, o la registra si no existe.
     *
     * <p>
     * Inserta si falta y después busca, en lugar de buscar, crear y capturar el error:
     * dos peticiones que crean la misma marca a la vez obtienen la misma fila sin que
     * ninguna transacción quede abortada. "diana" reutiliza "Diana".
     * </p>
     */
    @Transactional
    public Brand resolveByName(String name) {
        String trimmedName = name.trim();
        brandRepository.insertIfAbsent(trimmedName);
        return brandRepository.findByNameMatchingIndex(trimmedName)
                .orElseThrow(() -> new IllegalStateException(
                        BRAND_NOT_FOUND_AFTER_INSERT_MESSAGE.formatted(trimmedName)));
    }
}