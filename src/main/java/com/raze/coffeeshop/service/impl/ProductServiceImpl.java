package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.ProductRequest;
import com.raze.coffeeshop.dto.ProductResponse;
import com.raze.coffeeshop.exception.DuplicateResourceException;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Category;
import com.raze.coffeeshop.model.Product;
import com.raze.coffeeshop.repository.CategoryRepository;
import com.raze.coffeeshop.repository.ProductRepository;
import com.raze.coffeeshop.service.ProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio encargado de manejar la lógica de negocio para los productos.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Recupera todos los productos registrados en el sistema.
     *
     * @return Lista de {@link ProductResponse}
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Integer categoryId, Pageable pageable) {
        var page = categoryId == null
                ? productRepository.findByActiveTrue(pageable)
                : productRepository.findByActiveTrueAndCategoryId(categoryId, pageable);
        return page.map(this::toResponse);
    }

    /**
     * Busca un producto por su identificador UUID.
     *
     * @param id Identificador único del producto
     * @return {@link ProductResponse} con los datos del producto
     * @throws ResourceNotFoundException si el producto no se encuentra
     */
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(getProduct(id));
    }

    /**
     * Crea un nuevo producto validando que no exista otro con el mismo nombre y
     * que la categoría asociada exista.
     *
     * @param request Datos del nuevo producto
     * @return {@link ProductResponse} con los datos creados
     * @throws DuplicateResourceException si el nombre del producto ya está en uso
     * @throws ResourceNotFoundException si la categoría asignada no existe
     */
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

    /**
     * Actualiza la información de un producto existente.
     *
     * @param id Identificador UUID del producto a modificar
     * @param request Nuevos datos del producto
     * @return {@link ProductResponse} con los datos actualizados
     * @throws ResourceNotFoundException si el producto o la categoría no existen
     * @throws DuplicateResourceException si el nuevo nombre ya está en uso por otro producto
     */
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

    /**
     * Realiza un borrado lógico del producto, cambiándolo a estado inactivo.
     *
     * @param id Identificador del producto a desactivar
     * @throws ResourceNotFoundException si el producto no se encuentra
     */
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
