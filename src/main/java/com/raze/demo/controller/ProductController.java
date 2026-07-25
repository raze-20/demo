package com.raze.demo.controller;

import com.raze.demo.dto.ProductRequest;
import com.raze.demo.dto.ProductResponse;
import com.raze.demo.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de productos.
 * Proporciona endpoints para realizar operaciones CRUD sobre los productos del menú.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Obtiene la lista de todos los productos disponibles.
     *
     * @return Lista de {@link ProductResponse}
     */
    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    /**
     * Obtiene un producto por su identificador único (UUID).
     *
     * @param id Identificador UUID del producto
     * @return {@link ProductResponse} con los datos del producto
     */
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return productService.findById(id);
    }

    /**
     * Crea un nuevo producto en el catálogo.
     *
     * @param request Datos del producto a crear
     * @return {@link ResponseEntity} con el producto creado y el estado HTTP 201 Created
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity
                .created(URI.create("/api/products/" + response.id()))
                .body(response);
    }

    /**
     * Actualiza la información de un producto existente.
     *
     * @param id Identificador UUID del producto a actualizar
     * @param request Nuevos datos del producto
     * @return {@link ProductResponse} con los datos actualizados
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    /**
     * Elimina un producto del catálogo por su identificador.
     *
     * @param id Identificador UUID del producto a eliminar
     * @return {@link ResponseEntity} vacío con estado HTTP 204 No Content
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
