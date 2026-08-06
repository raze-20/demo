package com.raze.demo.service.impl;

import com.raze.demo.dto.ProductRequest;
import com.raze.demo.dto.ProductResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Category;
import com.raze.demo.model.Product;
import com.raze.demo.repository.CategoryRepository;
import com.raze.demo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link CustomerServiceImplTest}: Product
 * depende de dos repositorios (Product y Category), así que hay dos @Mock.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID productId;
    private Integer categoryId;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = 1;

        category = new Category();
        category.setId(categoryId);
        category.setName("Coffee");
        category.setActive(true);

        product = new Product();
        product.setId(productId);
        product.setName("Latte");
        product.setBasePrice(new BigDecimal("55.00"));
        product.setActive(true);
        // Sin JPA real, la relación @ManyToOne hay que enlazarla a mano; si no,
        // toResponse() del service revienta con NullPointerException al leer category.getId().
        product.setCategory(category);
    }

    @Test
    void findAll_devuelveSoloProductosActivos() {
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.findAll(null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Latte");
        assertThat(result.getContent().get(0).categoryName()).isEqualTo("Coffee");
        verify(productRepository).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void findAll_filtraPorCategoria_cuandoSeIndicaCategoryId() {
        when(productRepository.findByActiveTrueAndCategoryId(org.mockito.ArgumentMatchers.eq(categoryId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        Page<ProductResponse> result = productService.findAll(categoryId, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findByActiveTrueAndCategoryId(org.mockito.ArgumentMatchers.eq(categoryId), any(Pageable.class));
    }

    @Test
    void findById_devuelveProducto_cuandoExiste() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse result = productService.findById(productId);

        assertThat(result.basePrice()).isEqualByComparingTo("55.00");
        assertThat(result.categoryId()).isEqualTo(categoryId);
        verify(productRepository).findById(productId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(productRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.findById(missingId));
    }

    @Test
    void create_guardaYRetornaProducto_cuandoNombreDisponibleYCategoriaExiste() {
        ProductRequest request = new ProductRequest("Mocha", new BigDecimal("60.00"), true, categoryId);
        when(productRepository.findByNameIgnoreCase("Mocha")).thenReturn(Optional.empty());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse result = productService.create(request);

        assertThat(result).isNotNull();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_lanzaExcepcion_cuandoNombreYaExiste() {
        ProductRequest request = new ProductRequest("Latte", new BigDecimal("55.00"), true, categoryId);
        when(productRepository.findByNameIgnoreCase("Latte")).thenReturn(Optional.of(product));

        assertThrows(DuplicateResourceException.class, () -> productService.create(request));
    }

    @Test
    void create_lanzaExcepcion_cuandoCategoriaNoExiste() {
        ProductRequest request = new ProductRequest("Mocha", new BigDecimal("60.00"), true, categoryId);
        when(productRepository.findByNameIgnoreCase("Mocha")).thenReturn(Optional.empty());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.create(request));
    }

    @Test
    void update_actualizaYRetornaProducto_cuandoExiste() {
        ProductRequest request = new ProductRequest("Latte Grande", new BigDecimal("65.00"), true, categoryId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findByNameIgnoreCase("Latte Grande")).thenReturn(Optional.empty());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        ProductResponse result = productService.update(productId, request);

        assertThat(result.name()).isEqualTo("Latte Grande");
        assertThat(result.basePrice()).isEqualByComparingTo("65.00");
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        ProductRequest request = new ProductRequest("Latte Grande", new BigDecimal("65.00"), true, categoryId);
        when(productRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(missingId, request));
    }

    @Test
    void update_lanzaExcepcion_cuandoCategoriaNoExiste() {
        ProductRequest request = new ProductRequest("Latte Grande", new BigDecimal("65.00"), true, categoryId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.findByNameIgnoreCase("Latte Grande")).thenReturn(Optional.empty());
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(productId, request));
    }

    @Test
    void delete_marcaProductoComoInactivo() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.delete(productId);

        assertThat(product.getActive()).isFalse();
    }
}
