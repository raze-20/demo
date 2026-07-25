package com.raze.demo.repository;

import com.raze.demo.model.BranchInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, UUID> {

    List<BranchInventory> findByBranchId(UUID branchId);

    Optional<BranchInventory> findByBranchIdAndIngredientId(UUID branchId, UUID ingredientId);
}
