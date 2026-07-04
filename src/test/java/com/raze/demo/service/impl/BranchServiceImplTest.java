package com.raze.demo.service.impl;

import com.raze.demo.dto.BranchRequest;
import com.raze.demo.dto.BranchResponse;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private BranchServiceImpl branchService;

    private Branch branch;

    @BeforeEach
    void setUp() {
        branchID = UUID.randomUUID();
        branch = new Branch();
        branch.setId(branchID);
        branch.setName("Sucursal Centro");
        branch.setAddress("Av. Principal 123");
        branch.setCity("Ciudad de Mexico");
        branch.setState("CDMX");
    }

    @Test
    void findById_devuelveSucursal_cuandoExiste() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

        BranchResponse result = branchService.findById(1L);

        assertThat(result.getName()).isEqualTo("Sucursal Centro");
        verify(branchRepository).findById(1L);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> branchService.findById(99L));
    }

    @Test
    void create_guardaYRetornaSucursal() {
        BranchRequest request = new BranchRequest("Sucursal Norte", "Calle 45", "Monterrey", "NL");
        when(branchRepository.save(any(Branch.class))).thenReturn(branch);

        BranchResponse result = branchService.create(request);

        assertThat(result).isNotNull();
        verify(branchRepository).save(any(Branch.class));
    }
}