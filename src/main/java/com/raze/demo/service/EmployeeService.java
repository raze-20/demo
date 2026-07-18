package com.raze.demo.service;

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.dto.EmployeeUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    List<EmployeeResponse> findAll();

    EmployeeResponse findById(UUID userId);

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse update(UUID userId, EmployeeUpdateRequest request);

    void delete(UUID userId);
}
