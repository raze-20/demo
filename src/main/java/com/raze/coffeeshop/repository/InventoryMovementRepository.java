package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findByInventoryId(UUID inventoryId);
}
