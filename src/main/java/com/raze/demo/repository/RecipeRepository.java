package com.raze.demo.repository;

import com.raze.demo.model.Recipe;
import com.raze.demo.model.RecipeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, RecipeId> {
}
