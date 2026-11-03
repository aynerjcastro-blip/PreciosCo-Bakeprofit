package com.comprasco.bakeprofit.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Mis avisos más recientes junto con la cantidad total de no leídos (RF53). */
public record NotificationListResponse(
    @Schema(description = "Cantidad de avisos no leídos (de todos, no solo de los listados)")
    long unreadCount,

    @Schema(description = "Hasta 50 avisos, del más reciente al más antiguo")
    List<NotificationResponse> notifications
) {
}