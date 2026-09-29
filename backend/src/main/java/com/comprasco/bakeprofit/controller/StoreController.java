package com.comprasco.bakeprofit.controller;

import com.comprasco.bakeprofit.dto.StoreRequest;
import com.comprasco.bakeprofit.dto.StoreResponse;
import com.comprasco.bakeprofit.entity.Store;
import com.comprasco.bakeprofit.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;

@Tag(name = "Tiendas")
@RestController
@RequestMapping("api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @Operation(summary = "Listar todas las tiendas", description = "No filtra las tiendas")
    @ApiResponse(responseCode = "200", description = "Lista de todas las tiendas")
    @GetMapping
    public ResponseEntity<List<StoreResponse>> findAll() {
        return ResponseEntity.ok(storeService.findAll());
    }


    @Operation(summary = "Obtener tienda por ID", description = "Busca la tienda con el id indicado")
    @ApiResponse(responseCode = "200", description = "Tienda que tiene el id ingresado")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> findById(
                @Parameter(description = "Id de la tienda a buscar")
                @PathVariable Long id) {
        return ResponseEntity.ok(storeService.findByIdResponse(id));
    }


    @Operation(summary = "Buscar tiendas por nombre", description = "Filtra tiendas cuyo nombre coincida parcialmente con el texto indicado")
    @ApiResponse(responseCode = "200", description = "Lista de tiendas que coinciden con el filtro")
    @GetMapping("/search")
    public ResponseEntity<List<StoreResponse>> search(
                @Parameter(description = "Texto parcial del nombre de la tienda")
                @RequestParam String name) {
        return ResponseEntity.ok(storeService.searchByName(name));
    }


    @Operation(summary = "Listar tiendas activas", description = "Filtra tiendas por su estado")
    @ApiResponse(responseCode = "200", description = "Lista de tiendas activas")
    @GetMapping("/active")
    public ResponseEntity<List<StoreResponse>> findActive() {
        return ResponseEntity.ok(storeService.findActive());
    }


    @Operation(summary = "Listar tiendas inactivas", description = "Filtra tiendas por su estado")
    @ApiResponse(responseCode = "200", description = "Lista de tiendas inactivas")
    @GetMapping("/inactive")
    public ResponseEntity<List<StoreResponse>> findInactive() {
        return ResponseEntity.ok(storeService.findInactive());
    }


    @Operation(summary = "Registrar tienda", description = "Registra la nueva tienda en la base de datos")
    @ApiResponse(responseCode = "201", description = "Tienda registrada")
    @ApiResponse(responseCode = "400", description = "Validación fallida (o categoryId no es raíz)")
    @ApiResponse(responseCode = "404", description = "CategoryId no existe")
    @ApiResponse(responseCode = "409", description = "Ya existe una tienda con ese nombre o la categoría está inactivada")
    @PostMapping
    public ResponseEntity<StoreResponse> create(
                @Parameter(description = "Record con el nombre de la tienda e id de categoría")
                @Valid @RequestBody StoreRequest request) {
        StoreResponse response = storeService.create(request.name(), request.categoryId());
        return ResponseEntity
                .created(URI.create("/api/stores/" + response.id()))
                .body(response);
    }


    @Operation(summary = "Actualizar tienda", description = "Modifica el nombre e id de categoría de la tienda indicada")
    @ApiResponse(responseCode = "200", description = "Tienda actualizada")
    @ApiResponse(responseCode = "400", description = "Validación fallida (o categoryId no es raíz)")
    @ApiResponse(responseCode = "404", description = "Id o categoryId no existe")
    @ApiResponse(responseCode = "409", description = "Categoría inactiva")
    @PutMapping("/{id}")
    public ResponseEntity<StoreResponse> update(
                @Parameter(description = "Id de la tienda a actualizar")
                @PathVariable Long id,
                @Parameter(description = "Record con el nuevo nombre de la tienda e id de categoría")
                @Valid @RequestBody StoreRequest request) {
        return ResponseEntity.ok(storeService.update(id, request.name(), request.categoryId()));
    }


    @Operation(summary = "Desactivar tienda", description = "Busca la tienda con el id indicado y cambia su estado a inactivo")
    @ApiResponse(responseCode = "204", description = "Tienda desactivada")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
                @Parameter(description = "Id de la tienda")
                @PathVariable Long id) {
        storeService.deactivateStore(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Activar tienda", description = "Busca la tienda con el id indicado y cambia su estado a activo")
    @ApiResponse(responseCode = "204", description = "Tienda activada")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @ApiResponse(responseCode = "409", description = "Categoría inactiva")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(
                @Parameter(description = "Id de la tienda")
                @PathVariable Long id) {
        storeService.activateStore(id);
        return ResponseEntity.noContent().build();
    }
}