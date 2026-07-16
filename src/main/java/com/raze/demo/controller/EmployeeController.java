package com.raze.demo.controller;

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.service.EmployeeService;

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
 * Controlador REST para la gestión de empleados.
 * Proporciona endpoints para realizar operaciones CRUD sobre los perfiles de empleado.
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService service;

    /**
     * Obtiene la lista de todos los empleados registrados.
     *
     * @return Lista de {@link EmployeeResponse}
     */
    @GetMapping
    public List<EmployeeResponse> findAll() {
        // TODO change the method
        return service.findAll();
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
     * Crea un perfil de empleado para un usuario existente.
     *
     * @param request Datos del nuevo empleado
     * @return {@link ResponseEntity} con el empleado creado
     */
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/employees/" + response.userId())).body(response);
    }

    /**
     * Actualiza los datos de un empleado existente.
     *
     * @param userId  Identificador UUID del usuario asociado
     * @param request Nuevos datos del empleado
     * @return {@link EmployeeResponse} con los datos actualizados
     */
    @PutMapping("/{userId}")
    public EmployeeResponse update(@PathVariable UUID userId, @Valid @RequestBody EmployeeRequest request) {
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
