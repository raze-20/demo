package com.raze.coffeeshop.service;

import com.raze.coffeeshop.dto.BranchResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BranchService {

    public Page<BranchResponse> findAll(Pageable pageable);

    public BranchResponse findById(UUID id);

    public BranchResponse create(com.raze.coffeeshop.dto.BranchRequest request);

    public BranchResponse update(UUID id, com.raze.coffeeshop.dto.BranchRequest request);

    public void delete(UUID id);
}
