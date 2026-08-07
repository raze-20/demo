package com.raze.coffeeshop.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.raze.coffeeshop.dto.CategoryRequest;
import com.raze.coffeeshop.dto.CategoryResponse;

public interface CategoryService {

    public Page<CategoryResponse> findAll(Pageable pageable);

    public CategoryResponse findById(Integer id);

    public CategoryResponse create(CategoryRequest request);

    public CategoryResponse update(Integer id, CategoryRequest request);

    public void delete(Integer id);
}
