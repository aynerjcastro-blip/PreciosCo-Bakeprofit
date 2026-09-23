package com.comprasco.bakeprofit.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comprasco.bakeprofit.dto.PriceCompareResponse;
import com.comprasco.bakeprofit.entity.Price;
import com.comprasco.bakeprofit.repository.PriceRepository;

@Service
@Transactional(readOnly = true)
public class PriceService {

    private final PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    public List<PriceCompareResponse> compareByProduct(Long productId) {
        List<Price> prices = priceRepository.findRecentPricesByProduct(productId);

        return prices.stream()
                .map(p -> new PriceCompareResponse(
                        p.getProduct().getName(),
                        p.getStore().getName(),
                        p.getValue(),
                        p.getRegistrationDate()
                ))
                .toList();
    }
}