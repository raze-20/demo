package com.raze.demo.config;

import io.swagger.v3.oas.models.Operation;

import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Parameter;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Completa cada operación del documento OpenAPI con lo que no se deduce de la firma del método:
 * los roles exigidos por {@code @PreAuthorize} y los errores que devuelve
 * {@link com.raze.demo.exception.GlobalExceptionHandler}.
 *
 * <p>Va aquí y no como anotaciones {@code @ApiResponse} en los controllers porque son reglas
 * globales: repetirlas en cada endpoint es ruido que además se desincroniza.
 */
@Component
public class OpenApiOperationCustomizer implements GlobalOperationCustomizer {

    /** Extrae los roles de expresiones {@code hasRole('X')} y {@code hasAnyRole('X','Y')}. */
    private static final Pattern ROLE_EXPRESSION = Pattern.compile(
            "has(?:Any)?Role\\(\\s*(('[^']*'\\s*,?\\s*)+)\\)");

    private static final Pattern ROLE_NAME = Pattern.compile("'([^']*)'");

    /** Métodos que pueden chocar con una restricción de datos o con un bloqueo optimista. */
    private static final Set<RequestMethod> WRITE_METHODS =
            EnumSet.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        documentRequiredRoles(operation, handlerMethod);

        if (operation.getRequestBody() != null) {
            OpenApiConfig.addErrorResponse(operation, "400",
                    "Cuerpo de la petición mal formado o que incumple las validaciones. "
                            + "El campo `details` indica qué atributo falló y por qué.");
        }
        if (hasPathVariable(handlerMethod)) {
            OpenApiConfig.addErrorResponse(operation, "404",
                    "No existe ningún recurso con el identificador indicado.");
        }
        if (isWrite(handlerMethod)) {
            OpenApiConfig.addErrorResponse(operation, "409",
                    "Conflicto: el recurso ya existe, viola una restricción de integridad "
                            + "o fue modificado por otra operación mientras tanto.");
        }
        OpenApiConfig.addErrorResponse(operation, "500", "Error inesperado del servidor.");

        return operation;
    }

    /**
     * Añade a la descripción los roles que exige el endpoint, leyendo {@code @PreAuthorize} del
     * método o, si no lo tiene, de la clase del controller.
     *
     * <p>Cuando la expresión va más allá de una comprobación de roles (por ejemplo, permitir
     * también al dueño del recurso) se incluye tal cual, para no describir de menos el acceso real.
     */
    private void documentRequiredRoles(Operation operation, HandlerMethod handlerMethod) {
        PreAuthorize preAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), PreAuthorize.class);
        if (preAuthorize == null) {
            preAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), PreAuthorize.class);
        }
        if (preAuthorize == null) {
            return;
        }

        String expression = preAuthorize.value();
        Set<String> roles = extractRoles(expression);

        StringBuilder note = new StringBuilder();
        if (!roles.isEmpty()) {
            note.append("**Roles permitidos:** ").append(String.join(", ", roles));
        }
        if (!isPureRoleCheck(expression, roles)) {
            if (!note.isEmpty()) {
                note.append("\n\n");
            }
            note.append("**Regla de acceso:** `").append(expression).append('`');
        }

        String description = operation.getDescription();
        operation.setDescription(description == null || description.isBlank()
                ? note.toString()
                : description + "\n\n" + note);
    }

    private Set<String> extractRoles(String expression) {
        Set<String> roles = new LinkedHashSet<>();
        Matcher call = ROLE_EXPRESSION.matcher(expression);
        while (call.find()) {
            Matcher name = ROLE_NAME.matcher(call.group(1));
            while (name.find()) {
                roles.add(name.group(1));
            }
        }
        return roles;
    }

    /** {@code true} si la expresión no es más que la(s) llamada(s) a hasRole/hasAnyRole. */
    private boolean isPureRoleCheck(String expression, Set<String> roles) {
        return !roles.isEmpty() && ROLE_EXPRESSION.matcher(expression).replaceAll("").isBlank();
    }

    private boolean hasPathVariable(HandlerMethod handlerMethod) {
        for (Parameter parameter : handlerMethod.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(PathVariable.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWrite(HandlerMethod handlerMethod) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequestMapping.class);
        if (mapping == null) {
            return false;
        }
        for (RequestMethod method : mapping.method()) {
            if (WRITE_METHODS.contains(method)) {
                return true;
            }
        }
        return false;
    }
}
