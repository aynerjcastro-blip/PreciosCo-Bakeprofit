package com.comprasco.bakeprofit.controller;

import com.comprasco.bakeprofit.dto.CategoryRequest;
import com.comprasco.bakeprofit.dto.CategoryResponse;
import com.comprasco.bakeprofit.entity.Category;
import com.comprasco.bakeprofit.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;

@Tag(name = "Categorías")
@RestController
@RequestMapping("api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "Listar todas las categorías", description = "No filtra las categorías")
    @ApiResponse(responseCode = "200", description = "Lista de todas las categorías")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }


    @Operation(summary = "Obtener categoría por ID", description = "Busca la categoría con el id indicado")
    @ApiResponse(responseCode = "200", description = "Categoría que tiene el id ingresado")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(
                @Parameter(description = "Id de la categoría a buscar")
                @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findByIdResponse(id));
    }


    @Operation(summary = "Listar categorías activas", description = "Filtra categorías por su estado (solo subcategorías)")
    @ApiResponse(responseCode = "200", description = "Lista de categorías activas")
    @GetMapping("/active")
    public ResponseEntity<List<CategoryResponse>> findActive() {
        return ResponseEntity.ok(categoryService.findActive());
    }


    @Operation(summary = "Listar categorías inactivas", description = "Filtra categorías por su estado (solo subcategorías)")
    @ApiResponse(responseCode = "200", description = "Lista de categorías inactivas")
    @GetMapping("/inactive")
    public ResponseEntity<List<CategoryResponse>> findInactive() {
        return ResponseEntity.ok(categoryService.findInactive());
    }


    @Operation(summary = "Buscar categorías por nombre", description = "Filtra categorías cuyo nombre coincida parcialmente con el texto indicado (solo subcategorías)")
    @ApiResponse(responseCode = "200", description = "Lista de categorías que coinciden con el filtro")
    @GetMapping("/search")
    public ResponseEntity<List<CategoryResponse>> search(
                @Parameter(description = "Texto parcial del nombre de la categoría")
                @RequestParam String name) {
        return ResponseEntity.ok(categoryService.searchByName(name));
    }
    

    @Operation(summary = "Registrar categoría", description = "Registra la nueva categoría en la base de datos")
    @ApiResponse(responseCode = "201", description = "Categoría registrada")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "404", description = "ParentId no existe")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
                @Parameter(description = "Record con el nombre de la categoría, y id de categoría padre(cuando no es raíz)")
                @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request.name(), request.parentId());
        return ResponseEntity
                    .created(URI.create("/api/categories/" + response.id()))
                    .body(response);
    }


    @Operation(summary = "Actualizar categoría", description = """
            Modifica el nombre de la categoría indicada, y:
            - Si se ingresa parentId: se actualizará
            - Si no se ingresa parentId: permanecerá cómo estaba
            """)
    @ApiResponse(responseCode = "200", description = "Categoría actualizada")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "404", description = "Id o parentId no existe")
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
                @Parameter(description = "Id de la categoría a actualizar")
                @PathVariable Long id,
                @Parameter(description = "Record con el nuevo nombre de la categoría, y id de categoría padre(cuando no es raíz)")
                @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request.name(), request.parentId()));
    }


    @Operation(summary = "Desactivar categoría", description = """
            Busca la categoría con el id indicado y cambia su estado a inactivo, y:
            - Si es raíz: desactiva en cascada tiendas, categorías hijas y productos asociados a éstas
            - Si es hija: desactiva en cascada productos relacionados
            """)
    @ApiResponse(responseCode = "204", description = "Categoría desactivada")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(
                @Parameter(description = "Id de la categoría")
                @PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Activar categoría", description = """
            Busca la categoría con el id indicado y cambia su estado a activo, y:
            - Si es raíz: activa en cascada tiendas, categorías hijas y productos asociados a éstas
            - Si es hija: activa en cascada productos relacionados
            """)
    @ApiResponse(responseCode = "204", description = "Categoría activada")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(
                @Parameter(description = "Id de la categoría")
                @PathVariable Long id) {
        categoryService.activateCategory(id);
        return ResponseEntity.noContent().build();
    }
}