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
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Customer;
import com.raze.demo.model.User;
import com.raze.demo.repository.CustomerRepository;
import com.raze.demo.repository.UserRepository;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link BranchServiceImplTest}.
 *
 * No hay Spring, no hay base de datos: solo el objeto real CustomerServiceImpl con sus
 * dependencias (CustomerRepository, UserRepository) reemplazadas por mocks de Mockito.
 * La diferencia con Branch es que aquí Customer depende de DOS repositorios, así que hay
 * dos @Mock que @InjectMocks combina en un solo constructor al crear CustomerServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
// Sin esto, Mockito nunca inicializaría los @Mock/@InjectMocks de abajo (quedarían null)
// porque nadie llamaría a MockitoAnnotations.openMocks(this) por nosotros.
class CustomerServiceImplTest {

    @Mock
    // Doble falso de CustomerRepository: no toca Postgres, responde solo lo que
    // configuremos explícitamente con when(...) en cada test.
    private CustomerRepository customerRepository;

    @Mock
    // Segundo doble falso, para el segundo colaborador del service (busca el User
    // "dueño" del Customer antes de crear el perfil).
    private UserRepository userRepository;

    @InjectMocks
    // Mockito instancia CustomerServiceImpl de verdad y detecta, por tipo, qué mock va en
    // cada parámetro del constructor generado por @RequiredArgsConstructor.
    private CustomerServiceImpl customerService;

    private UUID customerId;
    private User user;
    private Customer customer;

    @BeforeEach
    // Se ejecuta antes de CADA @Test (no una vez por clase), para que cada test tenga
    // su propio User/Customer "limpios" y no se pisen datos entre pruebas.
    void setUp() {
        customerId = UUID.randomUUID();

        user = new User();
        user.setId(customerId);
        user.setEmail("cliente@example.com");
        user.setFirstName("Juan");
        user.setLastName("Perez");

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
    // Arrange (when) + Act (llamar al service real) + Assert (assertThat/verify).
    // No requiere ningún contexto de Spring para ejecutarse, solo el mock configurado.
    void findAll_devuelveSoloCustomersActivos() {
        when(customerRepository.findByActiveTrue()).thenReturn(List.of(customer));

        List<CustomerResponse> result = customerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("cliente@example.com");
        // Confirma que el service usó el método correcto del repositorio (no solo que
        // el resultado "por casualidad" coincide).
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
    // assertThrows ejecuta el lambda y verifica que lance justo esa excepción; si no la
    // lanza, o lanza otra distinta, el test falla. No necesita @ExceptionHandler ni MockMvc
    // porque aquí se está probando el service en Java puro, no la respuesta HTTP.
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(customerRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.findById(missingId));
    }

    @Test
    void create_guardaYRetornaCustomer_cuandoUsuarioExiste() {
        CustomerRequest request = new CustomerRequest(customerId, 5, LocalDate.of(2000, 1, 1));
        when(customerRepository.existsById(customerId)).thenReturn(false);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(user));
        // any(Customer.class): no nos importa el contenido exacto del objeto que el service
        // construye internamente, solo que al guardar "algún" Customer, el mock responda esto.
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        CustomerResponse result = customerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(customerId);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void create_lanzaExcepcion_cuandoUsuarioYaTieneCustomer() {
        CustomerRequest request = new CustomerRequest(customerId, 5, LocalDate.of(2000, 1, 1));
        when(customerRepository.existsById(customerId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> customerService.create(request));
    }

    @Test
    void create_lanzaExcepcion_cuandoUsuarioNoExiste() {
        CustomerRequest request = new CustomerRequest(customerId, 5, LocalDate.of(2000, 1, 1));
        when(customerRepository.existsById(customerId)).thenReturn(false);
        when(userRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerService.create(request));
    }

    @Test
    void update_actualizaYRetornaCustomer_cuandoExiste() {
        CustomerRequest request = new CustomerRequest(customerId, 20, LocalDate.of(1995, 5, 5));
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
        CustomerRequest request = new CustomerRequest(missingId, 20, LocalDate.of(1995, 5, 5));
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
