package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.IngredientRequest;
import com.raze.coffeeshop.dto.IngredientResponse;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Ingredient;
import com.raze.coffeeshop.repository.IngredientRepository;
import com.raze.coffeeshop.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService{

    private final IngredientRepository repository;

    /**
     * Recupera todos los ingredientes registrados en el sistema.
     *
     * @return Lista de {@link IngredientResponse}
     */
    @Transactional(readOnly = true)
    public Page<IngredientResponse> findAll(Pageable pageable) {
        return repository.findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IngredientResponse findById(UUID id) {
        return toResponse(getIngredient(id));
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.name());
        ingredient.setMeasureUnit(request.measureUnit());
        ingredient = repository.save(ingredient);
        return toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse update(UUID id, IngredientRequest request) {
        Ingredient ingredient = getIngredient(id);
        ingredient.setName(request.name());
        ingredient.setMeasureUnit(request.measureUnit());
        ingredient = repository.save(ingredient);
        return toResponse(ingredient);
    }

    @Transactional
    public void delete(UUID id) {
        Ingredient ingredient = getIngredient(id);
        ingredient.setActive(false);
        repository.save(ingredient);
    }

    private Ingredient getIngredient(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));
    }

    private IngredientResponse toResponse(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getMeasureUnit()
        );
    }

}
