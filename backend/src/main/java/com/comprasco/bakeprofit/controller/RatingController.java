package com.comprasco.bakeprofit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.config.TemporaryCurrentUser;
import com.comprasco.bakeprofit.dto.RatingRequest;
import com.comprasco.bakeprofit.dto.RatingResponse;
import com.comprasco.bakeprofit.dto.RatingSummaryResponse;
import com.comprasco.bakeprofit.service.RatingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Calificaciones")
@RestController
@RequestMapping("api/ratings")
public class RatingController {

    private final TemporaryCurrentUser currentUser;

    private final RatingService ratingService;

    public RatingController(RatingService ratingService, TemporaryCurrentUser currentUser) {
        this.ratingService = ratingService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Calificar un producto en una tienda",
            description = "Registra una calificación de 1 a 5 del usuario autenticado. Una por producto, tienda y usuario.")
    @ApiResponse(responseCode = "201", description = "Calificación registrada")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "401", description = "Sin sesión (pendiente de seguridad)")
    @ApiResponse(responseCode = "404", description = "Producto, tienda o precio del par no existe")
    @ApiResponse(responseCode = "409", description = "Ya calificaste este par, o el producto/tienda está inactivo")
    @PostMapping
    public ResponseEntity<RatingResponse> create(
            @Parameter(description = "Producto, tienda y puntaje (1 a 5)")
            @Valid @RequestBody RatingRequest request) {
        RatingResponse response = ratingService.create(
                currentUser.get(), request.productId(), request.storeId(), request.score());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Resumen de calificaciones",
            description = "Promedio y cantidad. Indica productId, storeId o ambos (producto en esa tienda).")
    @ApiResponse(responseCode = "200", description = "Promedio (null si no hay calificaciones) y cantidad")
    @ApiResponse(responseCode = "400", description = "No se indicó productId ni storeId")
    @ApiResponse(responseCode = "404", description = "Producto o tienda no existe")
    @GetMapping("/summary")
    public ResponseEntity<RatingSummaryResponse> summary(
            @Parameter(description = "Id del producto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Id de la tienda") @RequestParam(required = false) Long storeId) {
        return ResponseEntity.ok(ratingService.getSummary(productId, storeId));
    }

    @Operation(summary = "Mi calificación de un producto en una tienda",
            description = "Devuelve la calificación del usuario autenticado, o 204 si aún no calificó.")
    @ApiResponse(responseCode = "200", description = "Calificación del usuario")
    @ApiResponse(responseCode = "204", description = "El usuario aún no ha calificado ese par")
    @ApiResponse(responseCode = "401", description = "Sin sesión (pendiente de seguridad)")
    @ApiResponse(responseCode = "404", description = "Producto o tienda no existe")
    @GetMapping("/me")
    public ResponseEntity<RatingResponse> mine(
            @Parameter(description = "Id del producto") @RequestParam Long productId,
            @Parameter(description = "Id de la tienda") @RequestParam Long storeId) {
        return ratingService.findMine(currentUser.get(), productId, storeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
