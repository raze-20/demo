package com.raze.demo.service;

import com.raze.demo.dto.IngredientRequest;
import com.raze.demo.dto.IngredientResponse;

import java.util.List;
import java.util.UUID;

public interface IngredientService {

    List<IngredientResponse> findAll();

    IngredientResponse findById(UUID id);

    IngredientResponse create(IngredientRequest request);

    IngredientResponse update(UUID id, IngredientRequest request);

    void delete(UUID id);
}
