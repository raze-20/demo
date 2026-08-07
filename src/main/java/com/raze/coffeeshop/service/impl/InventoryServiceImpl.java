package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.BranchInventoryResponse;
import com.raze.coffeeshop.dto.InventoryMovementRequest;
import com.raze.coffeeshop.dto.InventoryMovementResponse;
import com.raze.coffeeshop.enums.MovementType;
import com.raze.coffeeshop.exception.InvalidStateException;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.BranchInventory;
import com.raze.coffeeshop.model.Ingredient;
import com.raze.coffeeshop.model.InventoryMovement;
import com.raze.coffeeshop.model.Recipe;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchInventoryRepository;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.IngredientRepository;
import com.raze.coffeeshop.repository.InventoryMovementRepository;
import com.raze.coffeeshop.repository.RecipeRepository;
import com.raze.coffeeshop.repository.UserRepository;
import com.raze.coffeeshop.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado del stock de ingredientes por sucursal: consulta, movimientos manuales
 * (entradas, mermas, ajustes) y el descuento automático que dispara la venta de un producto.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final RecipeRepository recipeRepository;
    private final BranchRepository branchRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BranchInventoryResponse> findByBranch(UUID branchId) {
        getBranch(branchId);
        return branchInventoryRepository.findByBranchId(branchId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryMovementResponse recordMovement(UUID branchId, InventoryMovementRequest request, UUID performedByUserId) {
        if (request.type() == MovementType.SALE) {
            throw new InvalidStateException("SALE movements are recorded automatically when an order is paid");
        }

        Branch branch = getBranch(branchId);
        Ingredient ingredient = getIngredient(request.ingredientId());
        User performedBy = getUser(performedByUserId);

        BranchInventory inventory = branchInventoryRepository.findByBranchIdAndIngredientId(branchId, request.ingredientId())
                .orElseGet(() -> {
                    BranchInventory created = new BranchInventory();
                    created.setBranch(branch);
                    created.setIngredient(ingredient);
                    return created;
                });

        applyDelta(inventory, request.type(), request.quantity());
        branchInventoryRepository.save(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(inventory);
        movement.setUser(performedBy);
        movement.setType(request.type());
        movement.setQuantity(request.quantity());
        movement.setReason(request.reason());
        movement = inventoryMovementRepository.save(movement);

        log.info("Inventory movement recorded: branch={}, ingredient={}, type={}, quantity={}",
                branchId, request.ingredientId(), request.type(), request.quantity());
        return toResponse(movement);
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> findMovements(UUID branchId, UUID ingredientId) {
        BranchInventory inventory = branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No inventory record for ingredient " + ingredientId + " at branch " + branchId));

        return inventoryMovementRepository.findByInventoryId(inventory.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void discountForSale(UUID branchId, UUID productId, int quantity, UUID orderId, UUID performedByUserId) {
        List<Recipe> recipeLines = recipeRepository.findByIdProductId(productId);
        if (recipeLines.isEmpty()) {
            return;
        }

        User performedBy = getUser(performedByUserId);
        for (Recipe line : recipeLines) {
            UUID ingredientId = line.getIngredient().getId();
            BigDecimal required = line.getRequiredQuantity().multiply(BigDecimal.valueOf(quantity));

            BranchInventory inventory = branchInventoryRepository.findByBranchIdAndIngredientId(branchId, ingredientId)
                    .orElseThrow(() -> new InvalidStateException(
                            "No inventory record for ingredient " + ingredientId + " at branch " + branchId));

            if (inventory.getCurrentQuantity().compareTo(required) < 0) {
                throw new InvalidStateException(
                        "Insufficient stock for ingredient " + ingredientId + " at branch " + branchId
                                + ": required " + required + ", available " + inventory.getCurrentQuantity());
            }

            inventory.setCurrentQuantity(inventory.getCurrentQuantity().subtract(required));
            branchInventoryRepository.save(inventory);

            InventoryMovement movement = new InventoryMovement();
            movement.setInventory(inventory);
            movement.setUser(performedBy);
            movement.setType(MovementType.SALE);
            movement.setQuantity(required);
            movement.setReason("Order " + orderId);
            inventoryMovementRepository.save(movement);
        }

        log.info("Inventory discounted for sale: branch={}, product={}, quantity={}, order={}",
                branchId, productId, quantity, orderId);
    }

    private void applyDelta(BranchInventory inventory, MovementType type, BigDecimal quantity) {
        if (type == MovementType.INCOMING) {
            inventory.setCurrentQuantity(inventory.getCurrentQuantity().add(quantity));
            return;
        }

        // WASTE y ADJUSTMENT siempre restan stock; la única forma de sumarlo es INCOMING.
        BigDecimal current = inventory.getCurrentQuantity();
        if (current.compareTo(quantity) < 0) {
            throw new InvalidStateException(
                    "Insufficient stock to record " + type + ": available " + current + ", requested " + quantity);
        }
        inventory.setCurrentQuantity(current.subtract(quantity));
    }

    private Branch getBranch(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
    }

    private Ingredient getIngredient(UUID id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingredient not found: " + id));
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private BranchInventoryResponse toResponse(BranchInventory inventory) {
        Ingredient ingredient = inventory.getIngredient();
        return new BranchInventoryResponse(
                inventory.getId(),
                inventory.getBranch().getId(),
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getMeasureUnit(),
                inventory.getCurrentQuantity(),
                inventory.getMinimumStock(),
                inventory.getLastUpdated()
        );
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getInventory().getIngredient().getId(),
                movement.getType(),
                movement.getQuantity(),
                movement.getReason(),
                movement.getUser().getId(),
                movement.getCreatedAt()
        );
    }
}
