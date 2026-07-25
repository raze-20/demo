package com.raze.demo.service.impl;

import com.raze.demo.dto.RecipeRequest;
import com.raze.demo.dto.RecipeResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Ingredient;
import com.raze.demo.model.Product;
import com.raze.demo.model.Recipe;
import com.raze.demo.model.RecipeId;
import com.raze.demo.repository.IngredientRepository;
import com.raze.demo.repository.ProductRepository;
import com.raze.demo.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link ProductServiceImplTest}: la receta
 * depende de tres repositorios (Recipe, Product, Ingredient).
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    private UUID productId;
    private UUID ingredientId;
    private Product product;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();

        product = new Product();
        product.setId(productId);
        product.setName("Latte");

        ingredient = new Ingredient();
        ingredient.setId(ingredientId);
        ingredient.setName("Leche entera");
        ingredient.setMeasureUnit("ml");
    }

    @Test
    void findByProduct_devuelveLasLineasDeLaReceta() {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setRequiredQuantity(new BigDecimal("150.000"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(recipeRepository.findByIdProductId(productId)).thenReturn(List.of(recipe));

        List<RecipeResponse> result = recipeService.findByProduct(productId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ingredientName()).isEqualTo("Leche entera");
        assertThat(result.get(0).requiredQuantity()).isEqualByComparingTo("150.000");
    }

    @Test
    void findByProduct_lanzaExcepcion_cuandoProductoNoExiste() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.findByProduct(productId));
    }

    @Test
    void addToProduct_guardaLaLinea_cuandoProductoEIngredienteExistenYNoDuplicada() {
        RecipeRequest request = new RecipeRequest(ingredientId, new BigDecimal("150.000"));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(recipeRepository.existsById(any(RecipeId.class))).thenReturn(false);
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponse response = recipeService.addToProduct(productId, request);

        assertThat(response.ingredientId()).isEqualTo(ingredientId);
        assertThat(response.requiredQuantity()).isEqualByComparingTo("150.000");
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void addToProduct_lanzaExcepcion_cuandoLaLineaYaExiste() {
        RecipeRequest request = new RecipeRequest(ingredientId, new BigDecimal("150.000"));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(recipeRepository.existsById(any(RecipeId.class))).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> recipeService.addToProduct(productId, request));
    }

    @Test
    void addToProduct_lanzaExcepcion_cuandoIngredienteNoExiste() {
        RecipeRequest request = new RecipeRequest(ingredientId, new BigDecimal("150.000"));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.addToProduct(productId, request));
    }

    @Test
    void removeFromProduct_borraLaLinea_cuandoExiste() {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        when(recipeRepository.findById(any(RecipeId.class))).thenReturn(Optional.of(recipe));

        recipeService.removeFromProduct(productId, ingredientId);

        verify(recipeRepository).delete(recipe);
    }

    @Test
    void removeFromProduct_lanzaExcepcion_cuandoLaLineaNoExiste() {
        when(recipeRepository.findById(any(RecipeId.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipeService.removeFromProduct(productId, ingredientId));
    }
}
