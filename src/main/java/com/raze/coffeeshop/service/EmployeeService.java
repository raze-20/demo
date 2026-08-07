package com.raze.coffeeshop.service;

import com.raze.coffeeshop.dto.EmployeeRequest;
import com.raze.coffeeshop.dto.EmployeeResponse;
import com.raze.coffeeshop.dto.EmployeeUpdateRequest;

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
