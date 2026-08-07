package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Page<Branch> findByActiveTrue(Pageable pageable);
}
