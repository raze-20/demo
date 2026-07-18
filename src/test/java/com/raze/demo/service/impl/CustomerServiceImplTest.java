package com.raze.demo.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link BranchServiceImplTest}.
 *
 * CustomerServiceImpl ahora registra el User (rol CUSTOMER fijo) y el Customer en el mismo
 * create(), así que depende de TRES colaboradores (Customer/User repos + PasswordEncoder).
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private UUID customerId;
    private User user;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        user = new User();
        user.setId(customerId);
        user.setEmail("cliente@example.com");
        user.setFirstName("Juan");
        user.setLastName("Perez");
        user.setRole(UserRole.CUSTOMER);

        customer = new Customer();
        customer.setUserId(customerId);
        // Importante: hay que enlazar el User al Customer manualmente porque, al no haber
        // JPA real, nadie hace ese "join" por nosotros; si se olvida, toResponse() del
        // service revienta con NullPointerException al leer customer.getUser().getEmail().
        customer.setUser(user);
        customer.setLoyaltyPoints(10);
        customer.setBirthDate(LocalDate.now());
        customer.setActive(true);
    }

    @Test
    void findAll_devuelveSoloCustomersActivos() {
        when(customerRepository.findByActiveTrue()).thenReturn(List.of(customer));

        List<CustomerResponse> result = customerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("cliente@example.com");
        verify(customerRepository).findByActiveTrue();
    }

    @Test
    void findById_devuelveCustomer_cuandoExiste() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        CustomerResponse result = customerService.findById(customerId);

        assertThat(result.loyaltyPoints()).isEqualTo(10);
        assertThat(result.email()).isEqualTo("cliente@example.com");
        verify(customerRepository).findById(customerId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.findById(missingId));
    }

    @Test
    // create() ahora registra el User (rol CUSTOMER forzado por el servicio, no por el
    // request) y el Customer en el mismo paso.
    void create_registraUsuarioYCustomer_cuandoEmailDisponible() {
        CustomerRequest request = new CustomerRequest("cliente@example.com", "password123", "Juan", "Perez", 5, LocalDate.of(2000, 1, 1));
        when(userRepository.findByEmailIgnoreCase("cliente@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        // any(Customer.class): no nos importa el contenido exacto del objeto que el service
        // construye internamente, solo que al guardar "algún" Customer, el mock responda esto.
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse result = customerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(customerId);
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_lanzaExcepcion_cuandoEmailYaExiste() {
        CustomerRequest request = new CustomerRequest("cliente@example.com", "password123", "Juan", "Perez", 5, LocalDate.of(2000, 1, 1));
        when(userRepository.findByEmailIgnoreCase("cliente@example.com")).thenReturn(Optional.of(user));

        assertThrows(DuplicateResourceException.class, () -> customerService.create(request));
    }

    @Test
    void update_actualizaYRetornaCustomer_cuandoExiste() {
        CustomerUpdateRequest request = new CustomerUpdateRequest(20, LocalDate.of(1995, 5, 5));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse result = customerService.update(customerId, request);

        assertThat(result.loyaltyPoints()).isEqualTo(20);
        assertThat(result.birthDate()).isEqualTo(LocalDate.of(1995, 5, 5));
        // Aquí sí verificamos con la instancia exacta "customer" (no any()), porque
        // update() modifica el mismo objeto que findById() devolvió.
        verify(customerRepository).save(customer);
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        CustomerUpdateRequest request = new CustomerUpdateRequest(20, LocalDate.of(1995, 5, 5));
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.update(missingId, request));
    }

    @Test
    void delete_marcaCustomerComoInactivo() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerService.delete(customerId);

        // No hay base de datos que consultar: el "borrado lógico" se comprueba leyendo
        // directamente el campo del objeto Java que el service mutó.
        assertThat(customer.getActive()).isFalse();
        verify(customerRepository).save(customer);
    }
}
