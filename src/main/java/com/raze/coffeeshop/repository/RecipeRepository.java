package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.Recipe;
import com.raze.coffeeshop.model.RecipeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, RecipeId> {

    List<Recipe> findByIdProductId(UUID productId);
}
