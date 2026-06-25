package com.raze.demo.service;

import com.raze.demo.dto.CategoryRequest;
import com.raze.demo.dto.CategoryResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Category;
import com.raze.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Integer id) {
        return toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        ensureNameIsAvailable(request.name(), null);

        Category category = new Category();
        category.setName(request.name().trim());
        category.setActive(request.active() == null || request.active());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = getCategory(id);
        ensureNameIsAvailable(request.name(), id);

        category.setName(request.name().trim());
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return toResponse(category);
    }

    @Transactional
    public void delete(Integer id) {
        Category category = getCategory(id);
        category.setActive(false);
    }

    private Category getCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private void ensureNameIsAvailable(String name, Integer currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim())
                .filter(category -> !category.getId().equals(currentId))
                .ifPresent(category -> {
                    throw new DuplicateResourceException("Category already exists: " + name);
                });
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getActive()
        );
    }
}
