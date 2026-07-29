package com.raze.demo.controller;

import com.raze.demo.dto.CategoryRequest;
import com.raze.demo.dto.CategoryResponse;
import com.raze.demo.service.CategoryService;

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

/**
 * Controlador REST para la gestión de categorías.
 * Proporciona endpoints para realizar operaciones CRUD sobre categorías.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Obtiene la lista de todas las categorías registradas.
     *
     * @return Lista de {@link CategoryResponse}
     */
    @GetMapping
    public Page<CategoryResponse> findAll(Pageable pageable) {
        return categoryService.findAll(pageable);
    }

    /**
     * Obtiene una categoría por su identificador único.
     *
     * @param id Identificador de la categoría
     * @return {@link CategoryResponse} con los datos de la categoría
     */
    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Integer id) {
        return categoryService.findById(id);
    }

    /**
     * Crea una nueva categoría.
     *
     * @param request Datos de la categoría a crear
     * @return {@link ResponseEntity} con la categoría creada y el estado HTTP 201 Created
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/categories/" + response.id()))
                .body(response);
    }

    /**
     * Actualiza los datos de una categoría existente.
     *
     * @param id Identificador de la categoría a actualizar
     * @param request Nuevos datos de la categoría
     * @return {@link CategoryResponse} con los datos actualizados
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Integer id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    /**
     * Elimina una categoría por su identificador.
     *
     * @param id Identificador de la categoría a eliminar
     * @return {@link ResponseEntity} vacío con estado HTTP 204 No Content
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
