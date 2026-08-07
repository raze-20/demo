package com.raze.coffeeshop.service;

import com.raze.coffeeshop.dto.UserRequest;
import com.raze.coffeeshop.dto.UserResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    Page<UserResponse> findAll(Pageable pageable);

    UserResponse findById(UUID id);

    UserResponse create(UserRequest request);

    UserResponse update(UUID id, UserRequest request);

    void delete(UUID id);
}
