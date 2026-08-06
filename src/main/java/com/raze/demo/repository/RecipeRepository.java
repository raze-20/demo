package com.raze.demo.repository;

import com.raze.demo.model.Recipe;
import com.raze.demo.model.RecipeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, RecipeId> {

    List<Recipe> findByIdProductId(UUID productId);
}
