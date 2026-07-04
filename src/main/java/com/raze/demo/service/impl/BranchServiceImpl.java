package com.raze.demo.service.impl;

import com.raze.demo.dto.BranchRequest;
import com.raze.demo.dto.BranchResponse;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    /**
     * Recupera todas las sucursales registradas en el sistema.
     *
     * @return Lista de {@link BranchResponse}
     */
    @Transactional(readOnly = true)
    public List<BranchResponse> findAll() {
        return branchRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse findById(UUID id) {
        return toResponse(getBranch(id));
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        Branch branch = new Branch();
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setCity(request.city());
        branch.setState(request.state());
        branch = branchRepository.save(branch);
        return toResponse(branch);
    }

    @Transactional
    public BranchResponse update(UUID id, BranchRequest request) {
        Branch branch = getBranch(id);
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setCity(request.city());
        branch.setState(request.state());
        branch = branchRepository.save(branch);
        return toResponse(branch);
    }

    @Transactional
    public void delete(UUID id) {
        Branch branch = getBranch(id);
        branch.setActive(false);
        branchRepository.save(branch);
    }

    private Branch getBranch(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
    }

    private BranchResponse toResponse(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getCity(),
                branch.getState(),
                branch.getCreatedAt()
        );
    }

}
