package com.comprasco.bakeprofit.catalog.controller;

import com.comprasco.bakeprofit.catalog.dto.LandingStatsResponse;
import com.comprasco.bakeprofit.catalog.service.LandingStatsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/landing")
public class LandingController {

    private final LandingStatsService service;

    public LandingController(LandingStatsService service) {
        this.service = service;
    }

    @Operation(summary = "Cifras reales para la página de inicio (público)")
    @GetMapping("/stats")
    public LandingStatsResponse stats() {
        return service.getStats();
    }
}

