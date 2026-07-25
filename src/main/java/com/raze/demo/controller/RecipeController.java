package com.raze.demo.controller;

import com.raze.demo.dto.RecipeRequest;
import com.raze.demo.dto.RecipeResponse;
import com.raze.demo.service.RecipeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la receta (bill of materials) de cada producto.
 */
@RestController
@RequestMapping("/api/products/{productId}/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public List<RecipeResponse> findAll(@PathVariable UUID productId) {
        return recipeService.findByProduct(productId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<RecipeResponse> create(
            @PathVariable UUID productId,
            @Valid @RequestBody RecipeRequest request
    ) {
        RecipeResponse response = recipeService.addToProduct(productId, request);
        return ResponseEntity
                .created(URI.create("/api/products/" + productId + "/recipes/" + response.ingredientId()))
                .body(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<Void> delete(@PathVariable UUID productId, @PathVariable UUID ingredientId) {
        recipeService.removeFromProduct(productId, ingredientId);
        return ResponseEntity.noContent().build();
    }
}
