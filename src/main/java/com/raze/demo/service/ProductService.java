package com.raze.demo.service;

import com.raze.demo.dto.ProductRequest;
import com.raze.demo.dto.ProductResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Category;
import com.raze.demo.model.Product;
import com.raze.demo.repository.CategoryRepository;
import com.raze.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        ensureNameIsAvailable(request.name(), null);

        Product product = new Product();
        product.setName(request.name().trim());
        product.setBasePrice(request.basePrice());
        product.setActive(request.active() == null || request.active());
        product.setCategory(getCategory(request.categoryId()));

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getProduct(id);
        ensureNameIsAvailable(request.name(), id);

        product.setName(request.name().trim());
        product.setBasePrice(request.basePrice());
        product.setCategory(getCategory(request.categoryId()));
        if (request.active() != null) {
            product.setActive(request.active());
        }

        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        Product product = getProduct(id);
        product.setActive(false);
    }

    private Product getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Category getCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private void ensureNameIsAvailable(String name, UUID currentId) {
        productRepository.findByNameIgnoreCase(name.trim())
                .filter(product -> !product.getId().equals(currentId))
                .ifPresent(product -> {
                    throw new DuplicateResourceException("Product already exists: " + name);
                });
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBasePrice(),
                product.getActive(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName()
        );
    }
}
