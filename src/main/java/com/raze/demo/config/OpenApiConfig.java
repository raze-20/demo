package com.raze.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger. Declara el esquema de seguridad JWT (Bearer) para que
 * Swagger UI (`/swagger-ui.html`) muestre el botón "Authorize" y envíe el header
 * {@code Authorization: Bearer <token>} en las pruebas contra los endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI coffeeDemoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coffee Demo API")
                        .description("Backend REST de una cafeteria: sucursales, catalogo, ventas, inventario.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
