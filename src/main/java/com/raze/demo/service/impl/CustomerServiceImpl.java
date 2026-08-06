package com.raze.demo.service.impl;

import com.raze.demo.dto.CustomerRequest;
import com.raze.demo.dto.CustomerResponse;
import com.raze.demo.dto.CustomerUpdateRequest;
import com.raze.demo.enums.UserRole;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Customer;
import com.raze.demo.model.User;
import com.raze.demo.repository.CustomerRepository;
import com.raze.demo.repository.UserRepository;
import com.raze.demo.service.CustomerService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio encargado de manejar la lógica de negocio para los clientes.
 * El alta crea el {@link User} (siempre con rol {@code CUSTOMER}) y su perfil de
 * {@link Customer} en el mismo paso.
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID userId) {
        return toResponse(getCustomer(userId));
    }

    /**
     * Registra un nuevo cliente: crea su {@link User} (con rol {@code CUSTOMER}) y, en la
     * misma transacción, su perfil de {@link Customer}.
     *
     * @param request Datos del usuario y del perfil de cliente
     * @return {@link CustomerResponse} con el cliente creado
     * @throws DuplicateResourceException si ya existe un usuario con el mismo correo
     */
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        ensureEmailIsAvailable(request.email());

        User user = new User();
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(UserRole.CUSTOMER);
        user = userRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(user);
        customer.setLoyaltyPoints(request.loyaltyPoints() == null ? 0 : request.loyaltyPoints());
        customer.setBirthDate(request.birthDate());
        customer = customerRepository.save(customer);
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID userId, CustomerUpdateRequest request) {
        Customer customer = getCustomer(userId);
        customer.setLoyaltyPoints(request.loyaltyPoints() == null ? 0 : request.loyaltyPoints());
        customer.setBirthDate(request.birthDate());
        customer = customerRepository.save(customer);
        return toResponse(customer);
    }

    @Transactional
    public void delete(UUID userId) {
        Customer customer = getCustomer(userId);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    private void ensureEmailIsAvailable(String email) {
        userRepository.findByEmailIgnoreCase(email.strip())
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User already exists: " + email);
                });
    }

    private Customer getCustomer(UUID userId) {
        return customerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + userId));
    }

    private CustomerResponse toResponse(Customer customer) {
        User user = customer.getUser();
        return new CustomerResponse(
                customer.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                customer.getLoyaltyPoints(),
                customer.getBirthDate()
        );
    }

}
