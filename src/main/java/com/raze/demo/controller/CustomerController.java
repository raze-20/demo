package com.raze.demo.controller;

import com.raze.demo.dto.CustomerRequest;
import com.raze.demo.dto.CustomerResponse;
import com.raze.demo.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de clientes.
 * Proporciona endpoints para realizar operaciones CRUD sobre los perfiles de cliente.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    /**
     * Obtiene la lista de todos los clientes registrados.
     *
     * @return Lista de {@link CustomerResponse}
     */
    @GetMapping
    public List<CustomerResponse> findAll() {
        //TODO cambiar a findByActiveTrue;
        return service.findAll();
    }

    /**
     * Obtiene un cliente por el identificador UUID de su usuario asociado.
     *
     * @param userId Identificador UUID del usuario
     * @return {@link CustomerResponse} con los datos del cliente
     */
    @GetMapping("/{userId}")
    public CustomerResponse findById(@PathVariable UUID userId) {
        return service.findById(userId);
    }

    /**
     * Crea un perfil de cliente para un usuario existente.
     *
     * @param request Datos del nuevo cliente
     * @return {@link ResponseEntity} con el cliente creado
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + response.userId())).body(response);
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param userId  Identificador UUID del usuario asociado
     * @param request Nuevos datos del cliente
     * @return {@link CustomerResponse} con los datos actualizados
     */
    @PutMapping("/{userId}")
    public CustomerResponse update(@PathVariable UUID userId, @Valid @RequestBody CustomerRequest request) {
        return service.update(userId, request);
    }

    /**
     * Elimina el perfil de cliente de un usuario.
     *
     * @param userId Identificador UUID del usuario asociado
     * @return {@link ResponseEntity} sin contenido
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable UUID userId) {
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }

}
