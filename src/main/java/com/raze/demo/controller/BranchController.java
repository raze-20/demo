package com.raze.demo.controller;

import com.raze.demo.dto.BranchRequest;
import com.raze.demo.dto.BranchResponse;
import com.raze.demo.service.BranchService;

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
 * Controlador REST para la gestión de sucursales.
 * Proporciona endpoints para realizar operaciones CRUD sobre las diferentes sucursales.
 */
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    /**
     * Obtiene la lista de todas las sucursales disponibles.
     *
     * @return Lista de {@link BranchResponse}
     */
    @GetMapping
    public List<BranchResponse> findAll() {
        return branchService.findAll();
    }

    /**
     * Obtiene una sucursal por su identificador único (UUID).
     *
     * @param id Identificador UUID de la sucursal
     * @return {@link BranchResponse} con los datos de la sucursal
     */
    @GetMapping("/{id}")
    public BranchResponse findById(@PathVariable UUID id) {
        return branchService.findById(id);
    }

    /**
     * Crea una nueva sucursal.
     *
     * @param request Datos de la nueva sucursal
     * @return {@link ResponseEntity} con la respuesta creada y ubicación
     */
    @PostMapping
    public ResponseEntity<BranchResponse> create(@Valid @RequestBody BranchRequest request) {
        BranchResponse response = branchService.create(request);
        return ResponseEntity.created(URI.create("/api/branches/" + response.id())).body(response);
    }

    /**
     * Actualiza los datos de una sucursal existente.
     *
     * @param id      Identificador UUID de la sucursal a actualizar
     * @param request Nuevos datos de la sucursal
     * @return {@link BranchResponse} con los datos actualizados
     */
    @PutMapping("/{id}")
    public BranchResponse update(@PathVariable UUID id, @Valid @RequestBody BranchRequest request) {
        return branchService.update(id, request);
    }

    /**
     * Elimina una sucursal del sistema de forma lógica o física según implementación.
     *
     * @param id Identificador UUID de la sucursal a eliminar
     * @return {@link ResponseEntity} sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
