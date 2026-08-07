package com.raze.coffeeshop.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.raze.coffeeshop.dto.ProductRequest;
import com.raze.coffeeshop.dto.ProductResponse;

public interface ProductService {

    public Page<ProductResponse> findAll(Integer categoryId, Pageable pageable);

    public ProductResponse findById(UUID id);

    public ProductResponse create(ProductRequest request);

    public ProductResponse update(UUID id, ProductRequest request);

    public void delete(UUID id);

    /* private Product getProduct(UUID id) {
        return null;
    }

    private Category getCategory(Integer id) {
        return null;
    }

    private void ensureNameIsAvailable(String name, UUID currentId);
 */

}
