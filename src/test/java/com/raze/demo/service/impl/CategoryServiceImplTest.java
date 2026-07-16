package com.raze.demo.service.impl;

import com.raze.demo.dto.CategoryRequest;
import com.raze.demo.dto.CategoryResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Category;
import com.raze.demo.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link BranchServiceImplTest}: solo
 * Mockito, sin Spring y sin base de datos. Category usa un id {@link Integer} (no UUID),
 * así que aquí conviene además cubrir la validación de nombre duplicado.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Integer categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryId = 1;
        category = new Category();
        category.setId(categoryId);
        category.setName("Coffee");
        category.setActive(true);
    }

    @Test
    void findAll_devuelveSoloCategoriasActivas() {
        when(categoryRepository.findByActiveTrue()).thenReturn(List.of(category));

        List<CategoryResponse> result = categoryService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Coffee");
        verify(categoryRepository).findByActiveTrue();
    }

    @Test
    void findById_devuelveCategoria_cuandoExiste() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.findById(categoryId);

        assertThat(result.name()).isEqualTo("Coffee");
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        Integer missingId = 999;
        when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(missingId));
    }

    @Test
    void create_guardaYRetornaCategoria_cuandoNombreDisponible() {
        CategoryRequest request = new CategoryRequest("Tea", true);
        when(categoryRepository.findByNameIgnoreCase("Tea")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    // ensureNameIsAvailable ignora la coincidencia si el id encontrado es el mismo que se
    // está creando/actualizando; con currentId=null en create(), cualquier categoría existente
    // con ese nombre debe disparar la excepción.
    void create_lanzaExcepcion_cuandoNombreYaExiste() {
        CategoryRequest request = new CategoryRequest("Coffee", true);
        when(categoryRepository.findByNameIgnoreCase("Coffee")).thenReturn(Optional.of(category));

        assertThrows(DuplicateResourceException.class, () -> categoryService.create(request));
    }

    @Test
    void update_actualizaYRetornaCategoria_cuandoExiste() {
        CategoryRequest request = new CategoryRequest("Coffee Beans", false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Coffee Beans")).thenReturn(Optional.empty());

        CategoryResponse result = categoryService.update(categoryId, request);

        assertThat(result.name()).isEqualTo("Coffee Beans");
        assertThat(result.active()).isFalse();
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        Integer missingId = 999;
        CategoryRequest request = new CategoryRequest("Coffee Beans", true);
        when(categoryRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.update(missingId, request));
    }

    @Test
    // El nombre existe, pero pertenece a LA MISMA categoría que se está actualizando
    // (currentId == category.getId()), así que no debería lanzar DuplicateResourceException.
    void update_permiteConservarSuPropioNombre() {
        CategoryRequest request = new CategoryRequest("Coffee", true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Coffee")).thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.update(categoryId, request);

        assertThat(result.name()).isEqualTo("Coffee");
    }

    @Test
    void update_lanzaExcepcion_cuandoNombreYaUsadoPorOtraCategoria() {
        Category otro = new Category();
        otro.setId(2);
        otro.setName("Tea");
        otro.setActive(true);

        CategoryRequest request = new CategoryRequest("Tea", true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByNameIgnoreCase("Tea")).thenReturn(Optional.of(otro));

        assertThrows(DuplicateResourceException.class, () -> categoryService.update(categoryId, request));
    }

    @Test
    void delete_marcaCategoriaComoInactiva() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        categoryService.delete(categoryId);

        assertThat(category.getActive()).isFalse();
    }
}
