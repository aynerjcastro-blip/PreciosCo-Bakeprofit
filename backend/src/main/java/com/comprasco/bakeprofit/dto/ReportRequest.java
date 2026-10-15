package com.comprasco.bakeprofit.dto;

import com.comprasco.bakeprofit.entity.ReportReason;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(

    @Schema(description = "Tipo de reporte", example = "SPAM")
    @NotNull(message = "El tipo de reporte es obligatorio")
    ReportReason reason,

    @Schema(description = "Detalle opcional (obligatorio si el tipo es OTRO), máximo 200 caracteres")
    @Size(max = 200, message = "El detalle no puede superar 200 caracteres")
    String details
) {
    // El vacío se guarda como null
    public ReportRequest {
        details = (details == null || details.isBlank()) ? null : details.trim();
    }
}
