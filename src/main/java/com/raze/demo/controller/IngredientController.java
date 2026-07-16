package com.raze.demo.controller;

import com.raze.demo.dto.IngredientRequest;
import com.raze.demo.dto.IngredientResponse;
import com.raze.demo.service.IngredientService;

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
 * Controlador REST para la gestión de ingredientes.
 * Proporciona endpoints para realizar operaciones CRUD sobre los ingredientes.
 */
@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService service;

    /**
     * Obtiene la lista de todas los ingredientes disponibles.
     *
     * @return Lista de {@link IngredientResponse}
     */
    @GetMapping
    public List<IngredientResponse> findAll() {
        return service.findAll();
    }

    /**
     * Obtiene un ingrediente por su identificador único (UUID).
     *
     * @param id Identificador UUID del ingrediente
     * @return {@link IngredientResponse} con los datos del ingrediente
     */
    @GetMapping("/{id}")
    public IngredientResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * Crea un nuevo ingrediente.
     *
     * @param request Datos del nuevo ingrediente
     * @return {@link ResponseEntity} con la respuesta creada
     */
    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientRequest request) {
        IngredientResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/ingredients/" + response.id())).body(response);
    }

    /**
     * Actualiza los datos de un ingrediente existente.
     *
     * @param id      Identificador UUID del ingrediente a actualizar
     * @param request Nuevos datos del ingrediente
     * @return {@link IngredientResponse} con los datos actualizados
     */
    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable UUID id, @Valid @RequestBody IngredientRequest request) {
        return service.update(id, request);
    }

    /**
     * Elimina un ingrediente del sistema de forma lógica.
     *
     * @param id Identificador UUID del ingrediente a eliminar
     * @return {@link ResponseEntity} sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
