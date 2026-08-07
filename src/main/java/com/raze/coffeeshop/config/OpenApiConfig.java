package com.raze.coffeeshop.config;

import com.raze.coffeeshop.exception.ApiError;
import com.raze.coffeeshop.exception.GlobalExceptionHandler;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * Configuración de OpenAPI/Swagger. Declara el esquema de seguridad JWT (Bearer) para que
 * Swagger UI (`/swagger-ui.html`) muestre el botón "Authorize" y envíe el header
 * {@code Authorization: Bearer <token>} en las pruebas contra los endpoints protegidos.
 *
 * <p>Las descripciones de cada endpoint no se declaran aquí: salen del Javadoc de los controllers
 * gracias a therapi-runtime-javadoc. Las respuestas de error las inyectan
 * {@link OpenApiOperationCustomizer} y {@link OpenApiSecurityCustomizer}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    /** Nombre del schema de {@link ApiError} dentro de {@code components.schemas}. */
    static final String API_ERROR_SCHEMA = "ApiError";

    @Bean
    public OpenAPI coffeeShopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coffee Shop API")
                        .description("Backend REST de una cafeteria: sucursales, catalogo, ventas, inventario.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * Registra el schema de {@link ApiError}, al que apuntan todas las respuestas de error.
     *
     * <p>Va sobre el documento ya construido y no sobre el bean {@link #coffeeShopOpenApi()}
     * porque springdoc reconstruye {@code components.schemas} durante el escaneo: lo que se
     * registre antes se pierde. {@code ApiError} no lo descubre solo, ya que ningún controller lo
     * declara como tipo de retorno; solo lo devuelve {@link GlobalExceptionHandler}.
     */
    @Bean
    public GlobalOpenApiCustomizer apiErrorSchemaCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            Components target = components;
            ModelConverters.getInstance().readAll(ApiError.class).forEach((name, resolved) -> {
                // Las propiedades salen del resolver, pero el schema se recrea como ObjectSchema:
                // el que devuelve el resolver se serializa sin "type" y sin él los generadores de
                // clientes no saben qué construir.
                Schema<?> source = resolved;
                ObjectSchema schema = new ObjectSchema();
                schema.setDescription(source.getDescription());
                Map<String, Schema> properties = source.getProperties();
                if (properties != null) {
                    properties.forEach(schema::addProperty);
                }
                target.addSchemas(name, schema);
            });
        };
    }

    /**
     * Añade una respuesta de error a una operación, salvo que ya esté documentada.
     *
     * <p>Todas apuntan al mismo schema {@code ApiError}, que es lo que realmente devuelve
     * {@link GlobalExceptionHandler}.
     *
     * @param operation operación del documento OpenAPI a completar
     * @param code código de estado HTTP
     * @param description qué significa ese error en esta operación
     */
    static void addErrorResponse(Operation operation, String code, String description) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        if (responses.containsKey(code)) {
            return;
        }
        responses.addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        MediaType.APPLICATION_JSON_VALUE,
                        new io.swagger.v3.oas.models.media.MediaType()
                                .schema(new Schema<>().$ref(
                                        Components.COMPONENTS_SCHEMAS_REF + API_ERROR_SCHEMA)))));
    }
}
