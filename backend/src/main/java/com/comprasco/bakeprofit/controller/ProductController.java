package com.comprasco.bakeprofit.controller;

import com.comprasco.bakeprofit.dto.ProductResponse;
import com.comprasco.bakeprofit.dto.ProductRequest;
import com.comprasco.bakeprofit.service.ProductService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;

@Tag(name = "Productos")
@RestController
@RequestMapping("api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController (ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Listar todos los productos", description = "No filtra los productos")
    @ApiResponse(responseCode = "200", description = "Lista de todos los productos")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }


    @Operation(summary = "Obtener producto por ID", description = "Busca el producto con el id indicado")
    @ApiResponse(responseCode = "200", description = "Producto que tiene el id ingresado")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(
                @Parameter(description = "Id del producto a buscar") 
                @PathVariable Long id) {
        return ResponseEntity.ok(productService.findByIdResponse(id));
    }


    @Operation(summary = "Listar productos activos", description = "Filtra productos por su estado")
    @ApiResponse(responseCode = "200", description = "Lista de productos activos")
    @GetMapping("/active")
    public ResponseEntity<List<ProductResponse>> findActive() {
        return ResponseEntity.ok(productService.findActive());
    }


    @Operation(summary = "Listar productos inactivos", description = "Filtra productos por su estado")
    @ApiResponse(responseCode = "200", description = "Lista de productos inactivos")
    @GetMapping("/inactive")
    public ResponseEntity<List<ProductResponse>> findInactive() {
        return ResponseEntity.ok(productService.findInactive());
    }


    @Operation(summary = "Buscar productos con filtros", 
            description = "Filtra productos activos por nombre y/o categoría. Ambos parámetros son opcionales.")
    @ApiResponse(responseCode = "200", description = "Lista de productos que coinciden con el filtro")
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> search(
            @Parameter(description = "Texto parcial del nombre del producto") 
            @RequestParam(required = false) String name,
            @Parameter(description = "ID de la categoría para filtrar") 
            @RequestParam(required = false) Long idCategory) {
        return ResponseEntity.ok(productService.search(name, idCategory));
    }


    @Operation(summary = "Registrar productos", description = "Registra el nuevo producto en la base de datos")
    @ApiResponse(responseCode = "201", description = "Producto registrado")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @PostMapping
    public ResponseEntity<ProductResponse> create (
                @Parameter(description = "Record con toda la información del producto(nombre, unidades, id de su categoria)")
                @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request.name(), request.unit(), request.idCategory());
        return ResponseEntity
                .created(URI.create("/api/products/" + response.id()))
                .body(response);
    }


    @Operation(summary = "Actualizar producto", description = "Modifica la información del producto indicado")
    @ApiResponse(responseCode = "200", description = "Producto modificado")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update (
                @Parameter(description = "Id del producto que se va a actualizar")
                @PathVariable Long id, 
                @Parameter(description = "Record con los nuevos datos del producto(nombre, unidades, id de su categoria)")                                   
                @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request.name(), 
                                                request.unit(), request.idCategory()));
    }


    @Operation(summary = "Actualizar producto", 
            description = "Busca el producto con el id indicado y cambia su estado a activo")
    @ApiResponse(responseCode = "204", description = "")
    @PatchMapping("/{id}/activate")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    public ResponseEntity<Void> activate (
                @Parameter(description = "Id del producto") 
                @PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Actualizar producto", 
            description = "Busca el producto con el id indicado y cambia su estado a inactivo")
    @ApiResponse(responseCode = "204", description = "")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate (
                @Parameter(description = "Id del producto") 
                @PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }
}