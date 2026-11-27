package com.comprasco.bakeprofit.catalog.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comprasco.bakeprofit.catalog.service.ProductPrescriptionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Administración de la marca "requiere fórmula médica" (HU-39, RN-33). Sigue el
 * mismo estilo que activar/desactivar producto.
 */
@Tag(name = "Productos")
@RestController
@RequestMapping("api/products")
public class ProductPrescriptionController {

    private final ProductPrescriptionService prescriptionService;

    public ProductPrescriptionController(ProductPrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @Operation(summary = "Marcar como de fórmula médica",
            description = "El asistente de IA de droguería deja de enviar este producto al modelo (RN-33). "
                    + "Úsese para medicamentos de fórmula cuyo nombre no contiene el principio activo.")
    @ApiResponse(responseCode = "204", description = "Producto marcado")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PatchMapping("/{id}/prescription/require")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> require(@Parameter(description = "Id del producto") @PathVariable Long id) {
        prescriptionService.setRequiresPrescription(id, true);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Marcar como de venta libre",
            description = "Quita la marca de fórmula médica. Si el nombre contiene un principio activo de la "
                    + "lista de fórmula, el asistente lo seguirá excluyendo.")
    @ApiResponse(responseCode = "204", description = "Marca retirada")
    @ApiResponse(responseCode = "404", description = "Id no existe")
    @PatchMapping("/{id}/prescription/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> release(@Parameter(description = "Id del producto") @PathVariable Long id) {
        prescriptionService.setRequiresPrescription(id, false);
        return ResponseEntity.noContent().build();
    }
}
