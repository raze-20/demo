package com.raze.demo.controller;

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.dto.EmployeeUpdateRequest;
import com.raze.demo.service.EmployeeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Controlador REST para la gestión de empleados.
 * Proporciona endpoints para realizar operaciones CRUD sobre los perfiles de empleado.
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class EmployeeController {

    private final EmployeeService service;

    /**
     * Obtiene la lista de todos los empleados registrados.
     *
     * @return Lista de {@link EmployeeResponse}
     */
    @GetMapping
    public Page<EmployeeResponse> findAll(Pageable pageable) {
        return service.findAll(pageable);
    }

    /**
     * Obtiene un empleado por el identificador UUID de su usuario asociado.
     *
     * @param userId Identificador UUID del usuario
     * @return {@link EmployeeResponse} con los datos del empleado
     */
    @GetMapping("/{userId}")
    public EmployeeResponse findById(@PathVariable UUID userId) {
        return service.findById(userId);
    }

    /**
     * Registra un nuevo empleado: crea su usuario (con el rol operativo indicado en
     * {@code type}) y su perfil de empleado en un solo paso.
     *
     * @param request Datos del usuario y del perfil de empleado
     * @return {@link ResponseEntity} con el empleado creado
     */
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/employees/" + response.userId())).body(response);
    }

    /**
     * Actualiza los datos de un empleado existente.
     *
     * @param userId  Identificador UUID del usuario asociado
     * @param request Nuevos datos del empleado
     * @return {@link EmployeeResponse} con los datos actualizados
     */
    @PutMapping("/{userId}")
    public EmployeeResponse update(@PathVariable UUID userId, @Valid @RequestBody EmployeeUpdateRequest request) {
        return service.update(userId, request);
    }

    /**
     * Elimina el perfil de empleado de un usuario.
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
