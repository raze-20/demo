package com.raze.coffeeshop.repository;

import com.raze.coffeeshop.model.Employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Page<Employee> findByActiveTrue(Pageable pageable);
}
