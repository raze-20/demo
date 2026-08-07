package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Page<User> findByActiveTrue(Pageable pageable);

    Optional<User> findByEmailIgnoreCase(String email);
}
