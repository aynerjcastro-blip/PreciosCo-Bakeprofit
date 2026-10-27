package com.comprasco.bakeprofit.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion global de OpenAPI / Swagger UI con soporte para autenticacion
 * JWT Bearer
 * y documentacion automatica de respuestas de seguridad (401 y 403).
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "PreciosCo API", version = "1.0", description = "Documentacion interactiva de la API REST para comparacion de precios y autenticacion"), security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "Ingrese el token JWT obtenido en el endpoint /api/auth/login")
public class OpenApiConfig {

    /**
     * Inyecta automaticamente las respuestas HTTP 401 y 403 en la documentacion de
     * todos los endpoints.
     *
     * @return OpenApiCustomizer con la declaracion de esquemas de respuesta de
     *         seguridad
     */
    @Bean
    public OpenApiCustomizer globalSecurityResponsesCustomizer() {
        return openApi -> openApi.getPaths().values()
                .forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                    ApiResponses apiResponses = operation.getResponses();
                    if (!apiResponses.containsKey("401")) {
                        apiResponses.addApiResponse("401", new ApiResponse()
                                .description("No autorizado: Token ausente, invalido o cuenta desactivada."));
                    }
                    if (!apiResponses.containsKey("403")) {
                        apiResponses.addApiResponse("403", new ApiResponse()
                                .description(
                                        "Prohibido: No tiene los permisos o roles necesarios para acceder a este recurso."));
                    }
                }));
    }
}