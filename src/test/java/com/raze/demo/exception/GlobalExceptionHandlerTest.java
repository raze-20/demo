package com.raze.demo.exception;

import com.raze.demo.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * TEST DE UNIDAD puro (sin contexto de Spring) para {@link GlobalExceptionHandler}: instancia
 * el handler directamente y verifica el status HTTP y el cuerpo {@link ApiError} para cada
 * excepción, incluyendo los casos endurecidos para producción (bloqueo optimista, violaciones
 * de integridad de datos, JSON malformado y excepciones no anticipadas).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_retorna404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(new ResourceNotFoundException("Order not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Order not found");
    }

    @Test
    void handleDuplicate_retorna409() {
        ResponseEntity<ApiError> response = handler.handleDuplicate(new DuplicateResourceException("Already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleInvalidState_retorna400() {
        ResponseEntity<ApiError> response = handler.handleInvalidState(new InvalidStateException("Invalid state"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleOptimisticLocking_retorna409_yNoExponeDetallesInternos() {
        UUID orderId = UUID.randomUUID();
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Order.class, orderId);

        ResponseEntity<ApiError> response = handler.handleOptimisticLocking(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).doesNotContain(orderId.toString());
    }

    @Test
    void handleDataIntegrityViolation_retorna409_conMensajeGenerico() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "ERROR: numeric field overflow (detalle interno de Postgres)");

        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).doesNotContain("Postgres");
    }

    @Test
    void handleMalformedJson_retorna400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("cuerpo invalido", mock(HttpInputMessage.class));

        ResponseEntity<ApiError> response = handler.handleMalformedJson(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleAuthentication_retorna401_sinExponerDetalles() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials for user ana@example.com");

        ResponseEntity<ApiError> response = handler.handleAuthentication(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).doesNotContain("ana@example.com");
    }

    @Test
    // Spring Security 7 lanza AuthorizationDeniedException (no la AccessDeniedException
    // "clásica") desde @PreAuthorize/@PostAuthorize; si este handler no la capturara
    // explícitamente, caería en el catch-all de Exception y devolvería 500 en vez de 403
    // (bug real detectado por el test de integración de auth).
    void handleAuthorizationDenied_retorna403() {
        AuthorizationDeniedException ex = new AuthorizationDeniedException("Access Denied");

        ResponseEntity<ApiError> response = handler.handleAuthorizationDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleUnexpected_retorna500_conMensajeSanitizado() {
        RuntimeException ex = new RuntimeException("detalle interno que no debe salir al cliente");

        ResponseEntity<ApiError> response = handler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("detalle interno");
    }
}
