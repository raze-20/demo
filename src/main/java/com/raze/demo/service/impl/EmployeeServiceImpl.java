package com.raze.demo.service.impl;

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.model.Employee;
import com.raze.demo.model.User;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.repository.EmployeeRepository;
import com.raze.demo.repository.UserRepository;
import com.raze.demo.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de manejar la lógica de negocio para los empleados.
 * Un empleado extiende un {@link User} existente mediante clave compartida (user_id).
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(UUID userId) {
        return toResponse(getEmployee(userId));
    }

    /**
     * Crea un perfil de empleado para un usuario existente en una sucursal.
     *
     * @param request Datos del nuevo empleado
     * @return {@link EmployeeResponse} con el empleado creado
     * @throws ResourceNotFoundException si el usuario o la sucursal referenciados no existen
     * @throws DuplicateResourceException si el usuario ya tiene un perfil de empleado
     */
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsById(request.userId())) {
            throw new DuplicateResourceException("Employee already exists for user: " + request.userId());
        }

        Employee employee = new Employee();
        employee.setUser(getUser(request.userId()));
        employee.setBranch(getBranch(request.branchId()));
        employee.setPosition(request.position());
        employee.setRole(request.role());
        employee.setHireDate(request.hireDate());
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse update(UUID userId, EmployeeRequest request) {
        Employee employee = getEmployee(userId);
        employee.setBranch(getBranch(request.branchId()));
        employee.setPosition(request.position());
        employee.setRole(request.role());
        employee.setHireDate(request.hireDate());
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public void delete(UUID userId) {
        Employee employee = getEmployee(userId);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private Employee getEmployee(UUID userId) {
        return employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + userId));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Branch getBranch(UUID branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));
    }

    private EmployeeResponse toResponse(Employee employee) {
        User user = employee.getUser();
        Branch branch = employee.getBranch();
        return new EmployeeResponse(
                employee.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName(),
                employee.getPosition(),
                employee.getRole(),
                employee.getHireDate()
        );
    }

}
