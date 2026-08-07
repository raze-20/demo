package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.RecipeRequest;
import com.raze.coffeeshop.dto.RecipeResponse;
import com.raze.coffeeshop.exception.DuplicateResourceException;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Ingredient;
import com.raze.coffeeshop.model.Product;
import com.raze.coffeeshop.model.Recipe;
import com.raze.coffeeshop.model.RecipeId;
import com.raze.coffeeshop.repository.IngredientRepository;
import com.raze.coffeeshop.repository.ProductRepository;
import com.raze.coffeeshop.repository.RecipeRepository;
import com.raze.coffeeshop.service.RecipeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de la receta (bill of materials) de cada producto: qué ingredientes y
 * en qué cantidad requiere producir una unidad de ese producto.
 */
@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public List<RecipeResponse> findByProduct(UUID productId) {
        getProduct(productId);
        return recipeRepository.findByIdProductId(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecipeResponse addToProduct(UUID productId, RecipeRequest request) {
        Product product = getProduct(productId);
        Ingredient ingredient = getIngredient(request.ingredientId());

        RecipeId id = recipeId(productId, request.ingredientId());
        if (recipeRepository.existsById(id)) {
            throw new DuplicateResourceException(
                    "Recipe already has ingredient " + request.ingredientId() + " for product " + productId);
        }

        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setRequiredQuantity(request.requiredQuantity());

        return toResponse(recipeRepository.save(recipe));
    }

    @Transactional
    public void removeFromProduct(UUID productId, UUID ingredientId) {
        RecipeId id = recipeId(productId, ingredientId);
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recipe not found for product " + productId + " and ingredient " + ingredientId));

        recipeRepository.delete(recipe);
    }

    private RecipeId recipeId(UUID productId, UUID ingredientId) {
        RecipeId id = new RecipeId();
        id.setProductId(productId);
        id.setIngredientId(ingredientId);
        return id;
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Ingredient getIngredient(UUID id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));
    }

    private RecipeResponse toResponse(Recipe recipe) {
        Ingredient ingredient = recipe.getIngredient();
        return new RecipeResponse(
                recipe.getProduct().getId(),
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getMeasureUnit(),
                recipe.getRequiredQuantity()
        );
    }
}
