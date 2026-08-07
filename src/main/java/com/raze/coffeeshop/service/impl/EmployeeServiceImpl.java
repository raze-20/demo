package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.EmployeeRequest;
import com.raze.coffeeshop.dto.EmployeeResponse;
import com.raze.coffeeshop.dto.EmployeeUpdateRequest;
import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.exception.DuplicateResourceException;
import com.raze.coffeeshop.exception.InvalidStateException;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.EmployeeRepository;
import com.raze.coffeeshop.repository.UserRepository;
import com.raze.coffeeshop.service.EmployeeService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio encargado de manejar la lógica de negocio para los empleados.
 * El alta crea el {@link User} y su perfil de {@link Employee} en el mismo paso, asignando
 * al usuario el rol operativo indicado como {@code type} (nunca {@code CUSTOMER}).
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> findAll(Pageable pageable) {
        return employeeRepository.findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(UUID userId) {
        return toResponse(getEmployee(userId));
    }

    /**
     * Registra un nuevo empleado: crea su {@link User} (con rol {@code request.type()}) y,
     * en la misma transacción, su perfil de {@link Employee} en la sucursal indicada.
     *
     * @param request Datos del usuario y del perfil de empleado
     * @return {@link EmployeeResponse} con el empleado creado
     * @throws InvalidStateException si {@code type} es {@code CUSTOMER}
     * @throws DuplicateResourceException si ya existe un usuario con el mismo correo
     * @throws ResourceNotFoundException si la sucursal referenciada no existe
     */
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        validateEmployeeType(request.type());
        ensureEmailIsAvailable(request.email());

        User user = new User();
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.type());
        user = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setBranch(getBranch(request.branchId()));
        employee.setPosition(request.position());
        employee.setRole(request.type().name());
        employee.setHireDate(request.hireDate());
        employee = employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse update(UUID userId, EmployeeUpdateRequest request) {
        validateEmployeeType(request.type());

        Employee employee = getEmployee(userId);
        employee.setBranch(getBranch(request.branchId()));
        employee.setPosition(request.position());
        employee.setRole(request.type().name());
        employee.setHireDate(request.hireDate());
        employee = employeeRepository.save(employee);

        User user = employee.getUser();
        user.setRole(request.type());
        userRepository.save(user);

        return toResponse(employee);
    }

    @Transactional
    public void delete(UUID userId) {
        Employee employee = getEmployee(userId);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private void validateEmployeeType(UserRole type) {
        if (type == UserRole.CUSTOMER) {
            throw new InvalidStateException("Employee type cannot be CUSTOMER: use /api/customers instead");
        }
    }

    private void ensureEmailIsAvailable(String email) {
        userRepository.findByEmailIgnoreCase(email.strip())
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User already exists: " + email);
                });
    }

    private Employee getEmployee(UUID userId) {
        return employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + userId));
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
