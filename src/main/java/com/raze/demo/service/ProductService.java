package com.raze.demo.service;

import java.util.List;
import java.util.UUID;

import com.raze.demo.dto.ProductRequest;
import com.raze.demo.dto.ProductResponse;

public interface ProductService {

    public List<ProductResponse> findAll();

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
