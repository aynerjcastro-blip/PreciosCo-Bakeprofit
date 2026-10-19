package com.comprasco.bakeprofit.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion global de OpenAPI / Swagger UI con soporte para autenticacion JWT Bearer.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PreciosCo API",
        version = "1.0",
        description = "Documentacion interactiva de la API REST"
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Ingrese el token JWT obtenido en el endpoint /api/auth/login"
)
public class OpenApiConfig {
}