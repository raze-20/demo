package com.raze.demo.service;

import com.raze.demo.dto.CustomerRequest;
import com.raze.demo.dto.CustomerResponse;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    List<CustomerResponse> findAll();

    CustomerResponse findById(UUID userId);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(UUID userId, CustomerRequest request);

    void delete(UUID userId);
}
