package com.raze.demo.controller;

import com.raze.demo.dto.UserRequest;
import com.raze.demo.dto.UserResponse;
import com.raze.demo.service.UserService;

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
 * Controlador REST para la gestión de usuarios.
 * Proporciona endpoints para realizar operaciones CRUD sobre los diferentes usuarios.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    /**
     * Obtiene la lista de todos los usuarios disponibles.
     *
     * @return Lista de {@link UserResponse}
     */
    @GetMapping
    public List<UserResponse> findAll() {
        // TODO change the query to findByActiveTrue;
        return service.findAll();
    }

    /**
     * Obtiene un usuario por su identificador único (UUID).
     *
     * @param id Identificador UUID del usuariio
     * @return {@link UserResponse} con los datos del usuario
     */
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    /**
     * Crea un nuevo usuario.
     *
     * @param request Datos del nuevo usuario
     * @return {@link ResponseEntity} con la respuesta creada
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * @param id      Identificador UUID del usuario a actualizar
     * @param request Nuevos datos del usuario
     * @return {@link UserResponse} con los datos actualizados
     */
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return service.update(id, request);
    }

    /**
     * Elimina un usuario del sistema de forma lógica.
     *
     * @param id Identificador UUID del usuario a eliminar
     * @return {@link ResponseEntity} sin contenido
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
