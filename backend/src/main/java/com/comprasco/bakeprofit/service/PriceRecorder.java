package com.comprasco.bakeprofit.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.comprasco.bakeprofit.entity.Price;
import com.comprasco.bakeprofit.entity.Product;
import com.comprasco.bakeprofit.entity.ScrapedOffer;
import com.comprasco.bakeprofit.repository.PriceRepository;

/**
 * Aplica la regla de precio repetido del historial por periodos (ver {@link Price}).
 * Corre dentro de la transacción de la oferta.
 */
@Component
public class PriceRecorder {

    private final PriceRepository priceRepository;

    public PriceRecorder(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    /**
     * Refresca el último periodo si el valor no cambió, o abre uno nuevo si cambió.
     *
     * @return el precio insertado, o vacío si solo se refrescó el periodo vigente
     */
    public Optional<Price> record(Product product, ScrapedOffer offer) {
        Optional<Price> latest = priceRepository
                .findFirstByProductIdAndStoreIdOrderByRegistrationDateDescIdDesc(
                        product.getId(), offer.getStore().getId());
        if (latest.isPresent() && latest.get().hasValue(offer.getPrice())) {
            latest.get().refreshSighting(offer.getProductUrl(), offer.getImageUrl());
            return Optional.empty();
        }
        // source es el nombre de la oferta en la tienda: es la convención de las filas existentes
        return Optional.of(priceRepository.save(Price.observed(
                product, offer.getStore(), offer.getPrice(), offer.getName(),
                offer.getProductUrl(), offer.getImageUrl())));
    }
}
