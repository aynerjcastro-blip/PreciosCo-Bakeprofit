package com.comprasco.bakeprofit.price.controller;

import com.comprasco.bakeprofit.price.dto.PriceCompareResponse;
import com.comprasco.bakeprofit.price.service.PriceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    
    @GetMapping("/compare")
    public ResponseEntity<List<PriceCompareResponse>> compare(
            @RequestParam Long productId) {
        return ResponseEntity.ok(priceService.compareByProduct(productId));
    }
}