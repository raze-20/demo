package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByNameIgnoreCase(String name);

    Page<Category> findByActiveTrue(Pageable pageable);
}
