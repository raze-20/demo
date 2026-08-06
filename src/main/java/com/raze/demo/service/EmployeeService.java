package com.raze.demo.service;

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.dto.EmployeeUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EmployeeService {

    Page<EmployeeResponse> findAll(Pageable pageable);

    EmployeeResponse findById(UUID userId);

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse update(UUID userId, EmployeeUpdateRequest request);

    void delete(UUID userId);
}
