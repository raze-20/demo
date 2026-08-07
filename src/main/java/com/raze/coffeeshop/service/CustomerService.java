package com.raze.coffeeshop.service;

import com.raze.coffeeshop.dto.CustomerRequest;
import com.raze.coffeeshop.dto.CustomerResponse;
import com.raze.coffeeshop.dto.CustomerUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    Page<CustomerResponse> findAll(Pageable pageable);

    CustomerResponse findById(UUID userId);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(UUID userId, CustomerUpdateRequest request);

    void delete(UUID userId);
}
