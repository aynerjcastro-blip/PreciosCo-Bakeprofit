package com.comprasco.bakeprofit.event;

import com.comprasco.bakeprofit.dto.CategoryTrackingMigrationDto;
import com.comprasco.bakeprofit.service.CategoryTrackingMigrationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Componente funcional encargado de coordinar y disparar el proceso 
 * de migración de seguimientos de productos hacia categorías.
 */
@Component
public class CategoryTrackingMigrationComponent {

    private final CategoryTrackingMigrationService migrationService;

    public CategoryTrackingMigrationComponent(CategoryTrackingMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Construye el DTO de migración y solicita al servicio que ejecute la actualización.
     *
     * @param userId Identificador del usuario.
     * @param genericProductId Identificador del producto genérico a desvincular.
     * @param categoryId Identificador de la nueva categoría a seguir.
     */
    public void executeMigrationProcess(Long userId, Long genericProductId, Long categoryId) {
        CategoryTrackingMigrationDto migrationDto = new CategoryTrackingMigrationDto(
            userId,
            genericProductId,
            categoryId,
            LocalDateTime.now()
        );

        boolean success = migrationService.migrateTrackingToCategory(migrationDto);

        if (!success) {
            System.err.println("Falló la ejecución de la migración de seguimiento para el usuario: " + userId);
        }
    }
}