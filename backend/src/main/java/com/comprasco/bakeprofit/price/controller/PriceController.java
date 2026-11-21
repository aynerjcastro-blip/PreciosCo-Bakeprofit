package com.comprasco.bakeprofit.price.controller;

import com.comprasco.bakeprofit.price.dto.PriceCompareResponse;
import com.comprasco.bakeprofit.price.service.PriceService;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="Precios")
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