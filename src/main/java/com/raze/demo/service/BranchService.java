package com.raze.demo.service;

import com.raze.demo.dto.BranchResponse;

import java.util.List;
import java.util.UUID;

public interface BranchService {

    public List<BranchResponse> findAll();

    public BranchResponse findById(UUID id);

    public BranchResponse create(com.raze.demo.dto.BranchRequest request);

    public BranchResponse update(UUID id, com.raze.demo.dto.BranchRequest request);

    public void delete(UUID id);
}
