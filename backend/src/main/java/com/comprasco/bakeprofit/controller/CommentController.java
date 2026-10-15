package com.comprasco.bakeprofit.controller;

import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.config.TemporaryCurrentUser;
import com.comprasco.bakeprofit.dto.CommentRequest;
import com.comprasco.bakeprofit.dto.CommentResponse;
import com.comprasco.bakeprofit.dto.ReportRequest;
import com.comprasco.bakeprofit.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Comentarios")
@RestController
@RequestMapping("api/comments")
public class CommentController {

    private final CommentService commentService;
    private final TemporaryCurrentUser currentUser;   // TODO(Ayner): reemplazar por CurrentUser

    public CommentController(CommentService commentService, TemporaryCurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Comentar un producto en una tienda",
            description = "Solo usuarios con rol USER. Entre 10 y 500 caracteres. Sin límite de comentarios por usuario.")
    @ApiResponse(responseCode = "201", description = "Comentario publicado")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "401", description = "Sin sesión (pendiente de seguridad)")
    @ApiResponse(responseCode = "403", description = "El rol del usuario no puede comentar")
    @ApiResponse(responseCode = "404", description = "Producto, tienda o precio del par no existe")
    @ApiResponse(responseCode = "409", description = "Producto o tienda inactivos")
    @PostMapping
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CommentRequest request) {
        CommentResponse response = commentService.create(
                currentUser.get(), request.productId(), request.storeId(), request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar comentarios visibles",
            description = "Indica productId, storeId o ambos. Orden: más recientes primero. Máximo 50 por página.")
    @ApiResponse(responseCode = "200", description = "Página de comentarios (en 'content')")
    @ApiResponse(responseCode = "400", description = "No se indicó productId ni storeId")
    @ApiResponse(responseCode = "404", description = "Producto o tienda no existe")
    @GetMapping
    public ResponseEntity<PagedModel<CommentResponse>> list(
            @Parameter(description = "Id del producto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Id de la tienda") @RequestParam(required = false) Long storeId,
            @Parameter(description = "Página (desde 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1 a 50)") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commentService.list(productId, storeId, page, size));
    }

    @Operation(summary = "Reportar un comentario",
            description = "Un reporte por usuario y comentario; repetirlo responde 204 sin crear otro. Con tipo OTRO el detalle es obligatorio.")
    @ApiResponse(responseCode = "204", description = "Reporte registrado")
    @ApiResponse(responseCode = "400", description = "Validación fallida o reporte del propio comentario")
    @ApiResponse(responseCode = "401", description = "Sin sesión (pendiente de seguridad)")
    @ApiResponse(responseCode = "404", description = "El comentario no existe o está oculto")
    @ApiResponse(responseCode = "409", description = "Reporte simultáneo duplicado")
    @PostMapping("/{id}/report")
    public ResponseEntity<Void> report(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        commentService.report(currentUser.get(), id, request.reason(), request.details());
        return ResponseEntity.noContent().build();
    }
}
