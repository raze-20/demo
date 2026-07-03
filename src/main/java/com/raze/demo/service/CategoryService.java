package com.raze.demo.service;

import java.util.List;

import com.raze.demo.dto.CategoryRequest;
import com.raze.demo.dto.CategoryResponse;

public interface CategoryService {

    public List<CategoryResponse> findAll();

    public CategoryResponse findById(Integer id);

    public CategoryResponse create(CategoryRequest request);

    public CategoryResponse update(Integer id, CategoryRequest request);

    public void delete(Integer id);
}
