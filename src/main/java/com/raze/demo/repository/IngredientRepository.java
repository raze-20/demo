package com.raze.demo.repository;

import com.raze.demo.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    List<Ingredient> findByActiveTrue();
}
