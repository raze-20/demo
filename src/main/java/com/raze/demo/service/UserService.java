package com.raze.demo.service;

import com.raze.demo.dto.UserRequest;
import com.raze.demo.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    List<UserResponse> findAll();

    UserResponse findById(UUID id);

    UserResponse create(UserRequest request);

    UserResponse update(UUID id, UserRequest request);

    void delete(UUID id);
}
