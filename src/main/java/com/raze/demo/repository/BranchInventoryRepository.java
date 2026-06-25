package com.raze.demo.repository;

import com.raze.demo.model.BranchInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchInventoryRepository extends JpaRepository<BranchInventory, UUID> {
}
