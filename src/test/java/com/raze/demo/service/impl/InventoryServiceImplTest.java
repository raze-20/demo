package com.raze.demo.service.impl;

import com.raze.demo.dto.InventoryMovementRequest;
import com.raze.demo.dto.InventoryMovementResponse;
import com.raze.demo.enums.MovementType;
import com.raze.demo.exception.InvalidStateException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.model.BranchInventory;
import com.raze.demo.model.Ingredient;
import com.raze.demo.model.InventoryMovement;
import com.raze.demo.model.Product;
import com.raze.demo.model.Recipe;
import com.raze.demo.model.User;
import com.raze.demo.repository.BranchInventoryRepository;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.repository.IngredientRepository;
import com.raze.demo.repository.InventoryMovementRepository;
import com.raze.demo.repository.RecipeRepository;
import com.raze.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer) del inventario: movimientos manuales y el descuento
 * automático que dispara la venta ({@code discountForSale}).
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private BranchInventoryRepository branchInventoryRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private UUID branchId;
    private UUID ingredientId;
    private UUID productId;
    private UUID userId;
    private UUID orderId;
    private Branch branch;
    private Ingredient ingredient;
    private Product product;
    private User user;

    @BeforeEach
    void setUp() {
        branchId = UUID.randomUUID();
        ingredientId = UUID.randomUUID();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        branch = new Branch();
        branch.setId(branchId);
        branch.setName("Sucursal Centro");

        ingredient = new Ingredient();
        ingredient.setId(ingredientId);
        ingredient.setName("Leche entera");
        ingredient.setMeasureUnit("ml");

        product = new Product();
        product.setId(productId);
        product.setName("Latte");

        user = new User();
        user.setId(userId);
    }

    private BranchInventory inventoryWith(BigDecimal current) {
        BranchInventory inventory = new BranchInventory();
        inventory.setId(UUID.randomUUID());
        inventory.setBranch(branch);
        inventory.setIngredient(ingredient);
        inventory.setCurrentQuantity(current);
        return inventory;
    }

    @Test
    void recordMovement_incoming_sumaStockYRegistraMovimiento() {
        InventoryMovementRequest request = new InventoryMovementRequest(
                ingredientId, MovementType.INCOMING, new BigDecimal("500.000"), "Compra semanal");
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.of(inventoryWith(new BigDecimal("100.000"))));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryMovementResponse response = inventoryService.recordMovement(branchId, request, userId);

        assertThat(response.type()).isEqualTo(MovementType.INCOMING);
        ArgumentCaptor<BranchInventory> captor = ArgumentCaptor.forClass(BranchInventory.class);
        verify(branchInventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentQuantity()).isEqualByComparingTo("600.000");
    }

    @Test
    void recordMovement_incoming_creaElRegistroDeInventario_siNoExiste() {
        InventoryMovementRequest request = new InventoryMovementRequest(
                ingredientId, MovementType.INCOMING, new BigDecimal("500.000"), null);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.empty());
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.recordMovement(branchId, request, userId);

        ArgumentCaptor<BranchInventory> captor = ArgumentCaptor.forClass(BranchInventory.class);
        verify(branchInventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentQuantity()).isEqualByComparingTo("500.000");
    }

    @Test
    void recordMovement_waste_restaStock() {
        InventoryMovementRequest request = new InventoryMovementRequest(
                ingredientId, MovementType.WASTE, new BigDecimal("30.000"), "Derrame");
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.of(inventoryWith(new BigDecimal("100.000"))));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.recordMovement(branchId, request, userId);

        ArgumentCaptor<BranchInventory> captor = ArgumentCaptor.forClass(BranchInventory.class);
        verify(branchInventoryRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentQuantity()).isEqualByComparingTo("70.000");
    }

    @Test
    void recordMovement_waste_lanzaExcepcion_siNoAlcanzaElStock() {
        InventoryMovementRequest request = new InventoryMovementRequest(
                ingredientId, MovementType.WASTE, new BigDecimal("300.000"), "Derrame");
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.of(inventoryWith(new BigDecimal("100.000"))));

        assertThrows(InvalidStateException.class, () -> inventoryService.recordMovement(branchId, request, userId));
    }

    @Test
    void recordMovement_sale_esRechazado() {
        InventoryMovementRequest request = new InventoryMovementRequest(
                ingredientId, MovementType.SALE, new BigDecimal("10.000"), null);

        assertThrows(InvalidStateException.class, () -> inventoryService.recordMovement(branchId, request, userId));
    }

    @Test
    void findByBranch_lanzaExcepcion_cuandoSucursalNoExiste() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.findByBranch(branchId));
    }

    @Test
    void discountForSale_restaStockYRegistraMovimientoSale() {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setRequiredQuantity(new BigDecimal("150.000"));

        BranchInventory inventory = inventoryWith(new BigDecimal("500.000"));
        when(recipeRepository.findByIdProductId(productId)).thenReturn(List.of(recipe));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.of(inventory));

        // 2 unidades del producto * 150 ml = 300 ml descontados; 500 - 300 = 200.
        inventoryService.discountForSale(branchId, productId, 2, orderId, userId);

        assertThat(inventory.getCurrentQuantity()).isEqualByComparingTo("200.000");
        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(MovementType.SALE);
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("300.000");
    }

    @Test
    void discountForSale_lanzaExcepcion_cuandoElStockNoAlcanza() {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setRequiredQuantity(new BigDecimal("150.000"));

        when(recipeRepository.findByIdProductId(productId)).thenReturn(List.of(recipe));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.of(inventoryWith(new BigDecimal("100.000"))));

        assertThrows(InvalidStateException.class,
                () -> inventoryService.discountForSale(branchId, productId, 1, orderId, userId));
    }

    @Test
    void discountForSale_lanzaExcepcion_cuandoNoHayRegistroDeInventario() {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setRequiredQuantity(new BigDecimal("150.000"));

        when(recipeRepository.findByIdProductId(productId)).thenReturn(List.of(recipe));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId))
                .thenReturn(Optional.empty());

        assertThrows(InvalidStateException.class,
                () -> inventoryService.discountForSale(branchId, productId, 1, orderId, userId));
    }

    @Test
    void discountForSale_noHaceNada_cuandoElProductoNoTieneReceta() {
        when(recipeRepository.findByIdProductId(productId)).thenReturn(List.of());

        inventoryService.discountForSale(branchId, productId, 1, orderId, userId);

        verify(branchInventoryRepository, never()).save(any());
        verify(inventoryMovementRepository, never()).save(any());
    }
}
