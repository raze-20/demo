    package com.raze.demo.service.impl;

import com.raze.demo.dto.CategoryRequest;
import com.raze.demo.dto.CategoryResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Category;
import com.raze.demo.repository.CategoryRepository;
import com.raze.demo.service.CategoryService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de manejar la lógica de negocio para las categorías.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Recupera todas las categorías registradas en la base de datos.
     *
     * @return Lista de {@link CategoryResponse}
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca una categoría por su identificador.
     *
     * @param id Identificador único de la categoría
     * @return {@link CategoryResponse} con los datos de la categoría encontrada
     * @throws ResourceNotFoundException si la categoría no existe
     */
    @Transactional(readOnly = true)
    public CategoryResponse findById(Integer id) {
        return toResponse(getCategory(id));
    }

    /**
     * Crea una nueva categoría validando que el nombre no esté duplicado.
     *
     * @param request Datos de la categoría a crear
     * @return {@link CategoryResponse} con la categoría creada
     * @throws DuplicateResourceException si ya existe una categoría con el mismo nombre
     */
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        ensureNameIsAvailable(request.name(), null);

        Category category = new Category();
        category.setName(request.name().trim());
        category.setActive(request.active() == null || request.active());

        return toResponse(categoryRepository.save(category));
    }

    /**
     * Actualiza los datos de una categoría existente.
     *
     * @param id Identificador de la categoría a actualizar
     * @param request Nuevos datos de la categoría
     * @return {@link CategoryResponse} con la categoría actualizada
     * @throws ResourceNotFoundException si la categoría no existe
     * @throws DuplicateResourceException si el nuevo nombre ya está en uso por otra categoría
     */
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

    /**
     * Realiza un borrado lógico de la categoría, desactivándola.
     *
     * @param id Identificador de la categoría a desactivar
     * @throws ResourceNotFoundException si la categoría no existe
     */
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
