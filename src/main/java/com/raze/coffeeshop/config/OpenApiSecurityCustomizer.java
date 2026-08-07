package com.raze.coffeeshop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Ajusta la seguridad de cada operación según lo que realmente hace {@link SecurityConfig}.
 *
 * <p>{@link OpenApiConfig} declara el JWT como requisito global, lo cual es cierto para casi toda
 * la API pero dejaba con candado a endpoints públicos como el login. Aquí se les quita el requisito
 * y, al resto, se les documentan el 401 y el 403.
 */
@Component
public class OpenApiSecurityCustomizer implements GlobalOpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) ->
                        customiseOperation(operation, HttpMethod.valueOf(method.name()), path)));
    }

    private void customiseOperation(Operation operation, HttpMethod method, String path) {
        if (PublicEndpoints.isPublic(method, path)) {
            // Una lista de seguridad vacía sobrescribe el requisito global: sin candado.
            operation.setSecurity(new ArrayList<>());
            return;
        }
        OpenApiConfig.addErrorResponse(operation, "401",
                "Falta el token, está expirado o no es válido.");
        OpenApiConfig.addErrorResponse(operation, "403",
                "El token es válido pero el usuario no tiene permiso para esta operación.");
    }
}
