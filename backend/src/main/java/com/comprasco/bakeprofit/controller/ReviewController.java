package com.comprasco.bakeprofit.controller;

import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.config.TemporaryCurrentUser;
import com.comprasco.bakeprofit.dto.RatingSummaryResponse;
import com.comprasco.bakeprofit.dto.ReportRequest;
import com.comprasco.bakeprofit.dto.ReviewCommentResponse;
import com.comprasco.bakeprofit.dto.ReviewRequest;
import com.comprasco.bakeprofit.dto.ReviewResponse;
import com.comprasco.bakeprofit.dto.ReviewUpdateRequest;
import com.comprasco.bakeprofit.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Opiniones (calificar y comentar)")
@RestController
@RequestMapping("api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final TemporaryCurrentUser currentUser;   // TODO(Ayner): reemplazar por CurrentUser

    public ReviewController(ReviewService reviewService, TemporaryCurrentUser currentUser) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Calificar y comentar un producto en una tienda",
            description = "Un solo paso (RN-30): calificación de 1 a 5 y comentario de 10 a 500 caracteres. "
                    + "Una opinión por producto, tienda y usuario. Solo rol USER.")
    @ApiResponse(responseCode = "201", description = "Opinión registrada")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no puede opinar")
    @ApiResponse(responseCode = "404", description = "Producto, tienda o precio del par no existe")
    @ApiResponse(responseCode = "409", description = "Ya opinaste, o el producto/tienda está inactivo")
    @PostMapping
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.create(
                currentUser.get(), request.productId(), request.storeId(), request.score(), request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Editar mi opinión",
            description = "Calificación y comentario juntos. Solo el autor, dentro del plazo desde el registro "
                    + "y mientras el comentario siga PUBLICADO.")
    @ApiResponse(responseCode = "200", description = "Opinión actualizada")
    @ApiResponse(responseCode = "400", description = "Validación fallida")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "No eres el autor")
    @ApiResponse(responseCode = "404", description = "La opinión no existe")
    @ApiResponse(responseCode = "409", description = "Plazo vencido o comentario reportado, oculto o eliminado")
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(
                reviewService.update(currentUser.get(), id, request.score(), request.text()));
    }

    @Operation(summary = "Eliminar mi comentario",
            description = "Borrado lógico en cualquier momento. La calificación se conserva y no puedes volver a opinar sobre ese producto en esa tienda.")
    @ApiResponse(responseCode = "204", description = "Comentario eliminado (repetir no hace nada)")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "No eres el autor")
    @ApiResponse(responseCode = "404", description = "La opinión no existe")
    @DeleteMapping("/{id}/comment")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        reviewService.deleteComment(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar opiniones con comentario visible",
            description = "Indica productId, storeId o ambos. Más recientes primero. Máximo 50 por página.")
    @ApiResponse(responseCode = "200", description = "Página de opiniones (en 'content')")
    @ApiResponse(responseCode = "400", description = "No se indicó productId ni storeId")
    @ApiResponse(responseCode = "404", description = "Producto o tienda no existe")
    @GetMapping
    public ResponseEntity<PagedModel<ReviewCommentResponse>> list(
            @Parameter(description = "Id del producto") @RequestParam(required = false) Long productId,
            @Parameter(description = "Id de la tienda") @RequestParam(required = false) Long storeId,
            @Parameter(description = "Página (desde 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página (1 a 50)") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.list(productId, storeId, page, size));
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
        return ResponseEntity.ok(reviewService.getSummary(productId, storeId));
    }

    @Operation(summary = "Mi opinión de un producto en una tienda",
            description = "Devuelve mi opinión (con el estado del comentario y hasta cuándo puedo editarla), o 204 si aún no opiné.")
    @ApiResponse(responseCode = "200", description = "Mi opinión")
    @ApiResponse(responseCode = "204", description = "Aún no has opinado sobre ese par")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "404", description = "Producto o tienda no existe")
    @GetMapping("/me")
    public ResponseEntity<ReviewResponse> mine(
            @Parameter(description = "Id del producto") @RequestParam Long productId,
            @Parameter(description = "Id de la tienda") @RequestParam Long storeId) {
        return reviewService.findMine(currentUser.get(), productId, storeId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Reportar el comentario de una opinión",
            description = "Un reporte por usuario y opinión; repetirlo responde 204 sin crear otro. Con razón OTRO el detalle es obligatorio.")
    @ApiResponse(responseCode = "204", description = "Reporte registrado")
    @ApiResponse(responseCode = "400", description = "Validación fallida o reporte del propio comentario")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "404", description = "La opinión no existe o su comentario no es visible")
    @ApiResponse(responseCode = "409", description = "Reporte simultáneo duplicado")
    @PostMapping("/{id}/report")
    public ResponseEntity<Void> report(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        reviewService.report(currentUser.get(), id, request.reason(), request.details());
        return ResponseEntity.noContent().build();
    }
}
