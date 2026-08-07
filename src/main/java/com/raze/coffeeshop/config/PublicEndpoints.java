package com.raze.coffeeshop.config;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

/**
 * Rutas accesibles sin autenticación.
 *
 * <p>Es la única fuente de verdad de esa lista: {@link SecurityConfig} la usa para construir la
 * cadena de filtros y {@link OpenApiSecurityCustomizer} para que Swagger UI no las marque con
 * candado. Si se declararan por separado, el documento OpenAPI terminaría mintiendo sobre qué
 * endpoints necesitan token.
 */
public final class PublicEndpoints {

    /**
     * Una ruta pública, opcionalmente limitada a un método HTTP.
     *
     * @param method método HTTP al que aplica, o {@code null} si aplica a todos
     * @param pattern patrón de ruta estilo Spring (admite {@code **} y variables {@code {id}})
     */
    public record Rule(HttpMethod method, String pattern) {
    }

    /** Endpoints de negocio abiertos a usuarios no autenticados. */
    public static final List<Rule> API = List.of(
            new Rule(null, "/api/v1/auth/**"),
            new Rule(HttpMethod.POST, "/api/v1/customers")
    );

    /** Documentación generada. No forma parte del propio documento OpenAPI. */
    public static final List<String> DOCS = List.of(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private static final List<PathPattern> API_PATTERNS = API.stream()
            .map(rule -> PathPatternParser.defaultInstance.parse(rule.pattern()))
            .toList();

    private PublicEndpoints() {
    }

    /**
     * Indica si una petición puede atenderse sin token.
     *
     * @param method método HTTP de la petición
     * @param path ruta de la petición; acepta la plantilla de OpenAPI ({@code /clientes/{id}})
     * @return {@code true} si alguna regla la declara pública
     */
    public static boolean isPublic(HttpMethod method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        for (int i = 0; i < API.size(); i++) {
            Rule rule = API.get(i);
            boolean methodMatches = rule.method() == null || rule.method().equals(method);
            if (methodMatches && API_PATTERNS.get(i).matches(container)) {
                return true;
            }
        }
        return false;
    }
}
