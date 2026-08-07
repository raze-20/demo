package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.BranchRequest;
import com.raze.coffeeshop.dto.BranchResponse;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<BranchResponse> findAll(Pageable pageable) {
        return branchRepository.findByActiveTrue(pageable)
                .map(this::toResponse);
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
