package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Page<Ingredient> findByActiveTrue(Pageable pageable);
}
