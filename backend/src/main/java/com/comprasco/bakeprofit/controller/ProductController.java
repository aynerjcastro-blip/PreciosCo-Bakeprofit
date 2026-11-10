package com.comprasco.bakeprofit.controller;

import com.comprasco.bakeprofit.dto.ProductResponse;
import com.comprasco.bakeprofit.dto.ProductRequest;
import com.comprasco.bakeprofit.dto.ProductSearchCriteria;
import com.comprasco.bakeprofit.service.ProductService;

import jakarta.validation.Valid;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Productos")
@RestController
@RequestMapping("api/products")
public class ProductController {

    private static final String DEFAULT_PAGE_SIZE = "20";

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
            description = "Filtra productos activos por nombre, categoría, tiendas y rango del precio vigente. "
                    + "Todos los parámetros son opcionales y se combinan (deben cumplirse todos). "
                    + "El nombre no distingue tildes ni mayúsculas y coincide si contiene todas las palabras. "
                    + "Sin tiendas, el rango se evalúa en todas las tiendas activas; con tiendas, solo en ellas. "
                    + "Ordenado por nombre. Máximo " + ProductService.MAX_PAGE_SIZE + " por página.")
    @ApiResponse(responseCode = "200", description = "Página de productos (en 'content'), también si no hay resultados")
    @ApiResponse(responseCode = "400", description = "Precio negativo o no numérico, o mínimo mayor que máximo")
    @ApiResponse(responseCode = "404", description = "Categoría inexistente, o tienda inexistente o inactiva")
    @GetMapping("/search")
    public ResponseEntity<PagedModel<ProductResponse>> search(
            @Parameter(description = "Palabras del nombre del producto")
            @RequestParam(required = false) String name,
            @Parameter(description = "ID de la categoría para filtrar")
            @RequestParam(required = false) Long idCategory,
            @Parameter(description = "ID de una tienda; se repite para varias (storeId=1&storeId=3)")
            @RequestParam(name = "storeId", required = false) List<Long> storeIds,
            @Parameter(description = "Precio vigente mínimo, inclusivo")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Precio vigente máximo, inclusivo")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Página (desde 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1 a " + ProductService.MAX_PAGE_SIZE + ")")
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        ProductSearchCriteria criteria =
                ProductSearchCriteria.of(name, idCategory, storeIds, minPrice, maxPrice);
        return ResponseEntity.ok(productService.search(criteria, page, size));
    }


    @Operation(summary = "Registrar productos", description = "Registra el nuevo producto en la base de datos")
    @ApiResponse(responseCode = "201", description = "Producto registrado")
    @ApiResponse(responseCode = "400", description = "Validación fallida; EAN inválido, de relleno, de uso interno de una tienda o de peso variable; o identidad incoherente (contenido sin unidad, referencia sin marca)")
    @ApiResponse(responseCode = "404", description = "CategoryId no existe")
    @ApiResponse(responseCode = "409", description = "Categoría inactiva, o EAN o marca + referencia ya registrados")
    @PostMapping
    public ResponseEntity<ProductResponse> create (
                @Parameter(description = "Record con toda la información del producto (nombre, unidades, id de su categoría e identidad opcional)")
                @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + response.id()))
                .body(response);
    }


    @Operation(summary = "Actualizar producto", description = "Modifica la información del producto indicado. "
            + "Si la petición no trae ningún campo de identidad (brandName, ean, netContent, unitBase, manufacturerRef), "
            + "se conserva la identidad existente. Si trae alguno, se reemplaza toda la identidad: "
            + "los campos de identidad omitidos quedan vacíos.")
    @ApiResponse(responseCode = "200", description = "Producto modificado")
    @ApiResponse(responseCode = "400", description = "Validación fallida; EAN inválido, de relleno, de uso interno de una tienda o de peso variable; o identidad incoherente (contenido sin unidad, referencia sin marca)")
    @ApiResponse(responseCode = "404", description = "Id o categoryId no existe")
    @ApiResponse(responseCode = "409", description = "Categoría inactiva, o EAN o marca + referencia ya registrados")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update (
                @Parameter(description = "Id del producto que se va a actualizar")
                @PathVariable Long id, 
                @Parameter(description = "Record con los nuevos datos del producto. Sin campos de identidad conserva la existente; con alguno, la reemplaza toda")
                @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }


    @Operation(summary = "Activar producto", 
            description = "Busca el producto con el id indicado y cambia su estado a activo")
    @ApiResponse(responseCode = "204", description = "")
    @PatchMapping("/{id}/activate")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @ApiResponse(responseCode = "409", description = "Categoría inactiva")
    public ResponseEntity<Void> activate (
                @Parameter(description = "Id del producto") 
                @PathVariable Long id) {
        productService.activateProduct(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Desactivar producto", 
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