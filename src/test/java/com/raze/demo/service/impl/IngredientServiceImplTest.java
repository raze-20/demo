package com.raze.demo.service.impl;

import com.raze.demo.dto.IngredientRequest;
import com.raze.demo.dto.IngredientResponse;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Ingredient;
import com.raze.demo.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link BranchServiceImplTest}. A
 * diferencia de Category/Product, IngredientServiceImpl llama a {@code repository.save(...)}
 * explícitamente en create/update/delete (no depende solo del dirty-checking de JPA), así que
 * aquí sí tiene sentido verificar esa llamada en cada caso.
 */
@ExtendWith(MockitoExtension.class)
class IngredientServiceImplTest {

    @Mock
    private IngredientRepository repository;

    @InjectMocks
    private IngredientServiceImpl ingredientService;

    private UUID ingredientId;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        ingredientId = UUID.randomUUID();
        ingredient = new Ingredient();
        ingredient.setId(ingredientId);
        ingredient.setName("Leche entera");
        ingredient.setMeasureUnit("ml");
        ingredient.setActive(true);
    }

    @Test
    void findAll_devuelveSoloIngredientesActivos() {
        when(repository.findByActiveTrue()).thenReturn(List.of(ingredient));

        List<IngredientResponse> result = ingredientService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Leche entera");
        verify(repository).findByActiveTrue();
    }

    @Test
    void findById_devuelveIngrediente_cuandoExiste() {
        when(repository.findById(ingredientId)).thenReturn(Optional.of(ingredient));

        IngredientResponse result = ingredientService.findById(ingredientId);

        assertThat(result.measureUnit()).isEqualTo("ml");
        verify(repository).findById(ingredientId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ingredientService.findById(missingId));
    }

    @Test
    void create_guardaYRetornaIngrediente() {
        IngredientRequest request = new IngredientRequest("Leche de almendra", "ml");
        when(repository.save(any(Ingredient.class))).thenReturn(ingredient);

        IngredientResponse result = ingredientService.create(request);

        assertThat(result).isNotNull();
        verify(repository).save(any(Ingredient.class));
    }

    @Test
    void update_actualizaYRetornaIngrediente_cuandoExiste() {
        IngredientRequest request = new IngredientRequest("Leche deslactosada", "l");
        when(repository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(repository.save(any(Ingredient.class))).thenReturn(ingredient);

        IngredientResponse result = ingredientService.update(ingredientId, request);

        assertThat(result.name()).isEqualTo("Leche deslactosada");
        assertThat(result.measureUnit()).isEqualTo("l");
        verify(repository).save(ingredient);
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        IngredientRequest request = new IngredientRequest("Leche deslactosada", "l");
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ingredientService.update(missingId, request));
    }

    @Test
    void delete_marcaIngredienteComoInactivo() {
        when(repository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(repository.save(any(Ingredient.class))).thenReturn(ingredient);

        ingredientService.delete(ingredientId);

        assertThat(ingredient.getActive()).isFalse();
        verify(repository).save(ingredient);
    }
}
