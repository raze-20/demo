package com.raze.demo.repository;

import com.raze.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    List<User> findByActiveTrue();

    Optional<User> findByEmailIgnoreCase(String email);
}
