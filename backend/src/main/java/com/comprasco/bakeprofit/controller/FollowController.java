package com.comprasco.bakeprofit.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.dto.FollowRequest;
import com.comprasco.bakeprofit.dto.FollowResponse;
import com.comprasco.bakeprofit.dto.FollowUpdateRequest;
import com.comprasco.bakeprofit.entity.User;
import com.comprasco.bakeprofit.security.CurrentUser;
import com.comprasco.bakeprofit.service.FollowService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Seguimiento de productos")
@RestController
@RequestMapping("api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @Operation(summary = "Seguir un producto",
            description = "Con precio objetivo opcional: sin él, se avisa ante cualquier reducción. Solo rol USER.")
    @ApiResponse(responseCode = "201", description = "Seguimiento creado")
    @ApiResponse(responseCode = "400", description = "Precio objetivo no positivo o datos inválidos")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no puede seguir productos")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    @ApiResponse(responseCode = "409", description = "Ya sigues el producto, o el producto está inactivo")
    @PostMapping
    public ResponseEntity<FollowResponse> create(@CurrentUser User user,
                                                 @Valid @RequestBody FollowRequest request) {
        FollowResponse response = followService.create(user, request.productId(), request.targetPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar mis seguimientos",
            description = "Solo los del usuario autenticado, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Lista de seguimientos (vacía si no sigues ninguno)")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no puede seguir productos")
    @GetMapping
    public ResponseEntity<List<FollowResponse>> list(@CurrentUser User user) {
        return ResponseEntity.ok(followService.list(user));
    }

    @Operation(summary = "Mi seguimiento de un producto",
            description = "Devuelve mi seguimiento del producto, o 204 si no lo sigo.")
    @ApiResponse(responseCode = "200", description = "Mi seguimiento")
    @ApiResponse(responseCode = "204", description = "No sigues ese producto")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no puede seguir productos")
    @GetMapping("/product/{productId}")
    public ResponseEntity<FollowResponse> mine(@CurrentUser User user, @PathVariable Long productId) {
        return followService.findMine(user, productId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Cambiar precio objetivo o activar/desactivar el aviso",
            description = "Reemplaza ambos valores. targetPrice null quita el objetivo (aviso ante cualquier reducción). "
                    + "Reactivar el aviso no genera avisos de bajas ocurridas mientras estuvo desactivado.")
    @ApiResponse(responseCode = "200", description = "Seguimiento actualizado")
    @ApiResponse(responseCode = "400", description = "Precio objetivo no positivo o datos inválidos")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no puede seguir productos")
    @ApiResponse(responseCode = "404", description = "El seguimiento no existe o no es tuyo")
    @PutMapping("/{id}")
    public ResponseEntity<FollowResponse> update(@CurrentUser User user, @PathVariable Long id,
                                                 @Valid @RequestBody FollowUpdateRequest request) {
        return ResponseEntity.ok(
                followService.update(user, id, request.notificationActive(), request.targetPrice()));
    }

    @Operation(summary = "Dejar de seguir un producto")
    @ApiResponse(responseCode = "204", description = "Seguimiento eliminado")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no puede seguir productos")
    @ApiResponse(responseCode = "404", description = "El seguimiento no existe o no es tuyo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser User user, @PathVariable Long id) {
        followService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}