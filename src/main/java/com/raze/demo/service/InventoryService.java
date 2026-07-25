package com.raze.demo.service;

import java.util.List;
import java.util.UUID;

import com.raze.demo.dto.BranchInventoryResponse;
import com.raze.demo.dto.InventoryMovementRequest;
import com.raze.demo.dto.InventoryMovementResponse;

public interface InventoryService {

    public List<BranchInventoryResponse> findByBranch(UUID branchId);

    public InventoryMovementResponse recordMovement(UUID branchId, InventoryMovementRequest request, UUID performedByUserId);

    public List<InventoryMovementResponse> findMovements(UUID branchId, UUID ingredientId);

    public void discountForSale(UUID branchId, UUID productId, int quantity, UUID orderId, UUID performedByUserId);

}
