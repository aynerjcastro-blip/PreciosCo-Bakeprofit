package com.comprasco.bakeprofit.controller;

import com.comprasco.bakeprofit.dto.RatingRequest;
import com.comprasco.bakeprofit.dto.RatingSummaryResponse;
import com.comprasco.bakeprofit.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<?> createRating(@Valid @RequestBody RatingRequest request) {
        // ID temporal de usuario de prueba (en producción proviene del JWT de Ayner)
        Long userId = 1L; 
        ratingService.createRating(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/summary")
    public ResponseEntity<RatingSummaryResponse> getSummary(@RequestParam Long productId, @RequestParam Long storeId) {
        return ResponseEntity.ok(ratingService.getSummary(productId, storeId));
    }
}