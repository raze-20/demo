package com.raze.demo.repository;

import com.raze.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByNameIgnoreCase(String name);
}
