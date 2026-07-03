package com.raze.demo.repository;

import com.raze.demo.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
}
