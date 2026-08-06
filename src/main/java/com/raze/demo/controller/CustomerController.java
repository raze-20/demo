package com.raze.demo.controller;

import com.raze.demo.dto.CustomerRequest;
import com.raze.demo.dto.CustomerResponse;
import com.raze.demo.dto.CustomerUpdateRequest;
import com.raze.demo.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Controlador REST para la gestión de clientes.
 * Proporciona endpoints para realizar operaciones CRUD sobre los perfiles de cliente.
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    /**
     * Obtiene la lista de todos los clientes registrados.
     *
     * @return Lista de {@link CustomerResponse}
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping
    public Page<CustomerResponse> findAll(@ParameterObject Pageable pageable) {
        return service.findAll(pageable);
    }

    /**
     * Obtiene un cliente por el identificador UUID de su usuario asociado.
     *
     * @param userId Identificador UUID del usuario
     * @return {@link CustomerResponse} con los datos del cliente
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #userId == authentication.principal.id")
    @GetMapping("/{userId}")
    public CustomerResponse findById(@P("userId") @PathVariable UUID userId) {
        return service.findById(userId);
    }

    /**
     * Registra un nuevo cliente: crea su usuario (rol {@code CUSTOMER}) y su perfil de
     * cliente en un solo paso.
     *
     * @param request Datos del usuario y del perfil de cliente
     * @return {@link ResponseEntity} con el cliente creado
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + response.userId())).body(response);
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param userId  Identificador UUID del usuario asociado
     * @param request Nuevos datos del cliente
     * @return {@link CustomerResponse} con los datos actualizados
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #userId == authentication.principal.id")
    @PutMapping("/{userId}")
    public CustomerResponse update(@P("userId") @PathVariable UUID userId, @Valid @RequestBody CustomerUpdateRequest request) {
        return service.update(userId, request);
    }

    /**
     * Elimina el perfil de cliente de un usuario.
     *
     * @param userId Identificador UUID del usuario asociado
     * @return {@link ResponseEntity} sin contenido
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or #userId == authentication.principal.id")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@P("userId") @PathVariable UUID userId) {
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }

}
