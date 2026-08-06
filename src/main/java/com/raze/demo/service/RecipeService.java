package com.raze.demo.service;

import java.util.List;
import java.util.UUID;

import com.raze.demo.dto.RecipeRequest;
import com.raze.demo.dto.RecipeResponse;

public interface RecipeService {

    public List<RecipeResponse> findByProduct(UUID productId);

    public RecipeResponse addToProduct(UUID productId, RecipeRequest request);

    public void removeFromProduct(UUID productId, UUID ingredientId);

}
