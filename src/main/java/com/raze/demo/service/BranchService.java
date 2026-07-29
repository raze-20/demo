package com.raze.demo.service;

import com.raze.demo.dto.BranchResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BranchService {

    public Page<BranchResponse> findAll(Pageable pageable);

    public BranchResponse findById(UUID id);

    public BranchResponse create(com.raze.demo.dto.BranchRequest request);

    public BranchResponse update(UUID id, com.raze.demo.dto.BranchRequest request);

    public void delete(UUID id);
}
