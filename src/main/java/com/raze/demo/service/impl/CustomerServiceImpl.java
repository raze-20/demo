package com.raze.demo.service.impl;

import com.raze.demo.dto.CustomerRequest;
import com.raze.demo.dto.CustomerResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Customer;
import com.raze.demo.model.User;
import com.raze.demo.repository.CustomerRepository;
import com.raze.demo.repository.UserRepository;
import com.raze.demo.service.CustomerService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de manejar la lógica de negocio para los clientes.
 * Un cliente extiende un {@link User} existente mediante clave compartida (user_id).
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID userId) {
        return toResponse(getCustomer(userId));
    }

    /**
     * Crea un perfil de cliente para un usuario existente.
     *
     * @param request Datos del nuevo cliente
     * @return {@link CustomerResponse} con el cliente creado
     * @throws ResourceNotFoundException si el usuario referenciado no existe
     * @throws DuplicateResourceException si el usuario ya tiene un perfil de cliente
     */
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsById(request.userId())) {
            throw new DuplicateResourceException("Customer already exists for user: " + request.userId());
        }

        Customer customer = new Customer();
        customer.setUser(getUser(request.userId()));
        customer.setLoyaltyPoints(request.loyaltyPoints() == null ? 0 : request.loyaltyPoints());
        customer.setBirthDate(request.birthDate());
        customer = customerRepository.save(customer);
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID userId, CustomerRequest request) {
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

    private Customer getCustomer(UUID userId) {
        return customerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + userId));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
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
