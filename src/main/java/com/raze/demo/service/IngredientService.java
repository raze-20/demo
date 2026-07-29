package com.raze.demo.service;

import com.raze.demo.dto.IngredientRequest;
import com.raze.demo.dto.IngredientResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IngredientService {

    Page<IngredientResponse> findAll(Pageable pageable);

    IngredientResponse findById(UUID id);

    IngredientResponse create(IngredientRequest request);

    IngredientResponse update(UUID id, IngredientRequest request);

    void delete(UUID id);
}
