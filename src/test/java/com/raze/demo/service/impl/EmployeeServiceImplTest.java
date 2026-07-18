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

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.dto.EmployeeUpdateRequest;
import com.raze.demo.enums.UserRole;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.InvalidStateException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.model.Employee;
import com.raze.demo.model.User;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.repository.EmployeeRepository;
import com.raze.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link CustomerServiceImplTest} y
 * {@link BranchServiceImplTest}: Mockito puro, sin Spring, sin base de datos.
 *
 * EmployeeServiceImpl ahora registra el User y el Employee en el mismo create(), así que
 * depende de CUATRO colaboradores (Employee/User/Branch repos + PasswordEncoder).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private UUID employeeId;
    private UUID branchId;
    private User user;
    private Branch branch;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        branchId = UUID.randomUUID();

        user = new User();
        user.setId(employeeId);
        user.setEmail("empleado@example.com");
        user.setFirstName("Ana");
        user.setLastName("Gomez");
        user.setRole(UserRole.BARISTA);

        branch = new Branch();
        branch.setId(branchId);
        branch.setName("Sucursal Centro");
        branch.setAddress("Av. Principal 123");
        branch.setCity("Ciudad de Mexico");
        branch.setState("CDMX");
        branch.setActive(true);

        employee = new Employee();
        employee.setUserId(employeeId);
        // Como no hay JPA real, las relaciones (@OneToOne, @ManyToOne) hay que
        // enlazarlas a mano; si no, toResponse() del service truena con NPE.
        employee.setUser(user);
        employee.setBranch(branch);
        employee.setPosition("Barista");
        employee.setRole("BARISTA");
        employee.setHireDate(LocalDate.of(2022, 3, 1));
        employee.setActive(true);
    }

    @Test
    void findAll_devuelveSoloEmployeesActivos() {
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(employee));

        List<EmployeeResponse> result = employeeService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("empleado@example.com");
        verify(employeeRepository).findByActiveTrue();
    }

    @Test
    void findById_devuelveEmployee_cuandoExiste() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

        EmployeeResponse result = employeeService.findById(employeeId);

        assertThat(result.position()).isEqualTo("Barista");
        assertThat(result.branchId()).isEqualTo(branchId);
        verify(employeeRepository).findById(employeeId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(employeeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.findById(missingId));
    }

    @Test
    // create() ahora registra el User (email libre, password hasheado) y el Employee en el
    // mismo paso, así que hay que simular email disponible, hash y la sucursal.
    void create_registraUsuarioYEmployee_cuandoDatosSonValidos() {
        EmployeeRequest request = new EmployeeRequest(
                "empleado@example.com", "password123", "Ana", "Gomez",
                UserRole.BARISTA, branchId, "Barista", LocalDate.of(2022, 3, 1));
        when(userRepository.findByEmailIgnoreCase("empleado@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse result = employeeService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(employeeId);
        verify(userRepository).save(any(User.class));
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void create_lanzaExcepcion_cuandoTypeEsCustomer() {
        EmployeeRequest request = new EmployeeRequest(
                "empleado@example.com", "password123", "Ana", "Gomez",
                UserRole.CUSTOMER, branchId, "Barista", LocalDate.of(2022, 3, 1));

        assertThrows(InvalidStateException.class, () -> employeeService.create(request));
    }

    @Test
    void create_lanzaExcepcion_cuandoEmailYaExiste() {
        EmployeeRequest request = new EmployeeRequest(
                "empleado@example.com", "password123", "Ana", "Gomez",
                UserRole.BARISTA, branchId, "Barista", LocalDate.of(2022, 3, 1));
        when(userRepository.findByEmailIgnoreCase("empleado@example.com")).thenReturn(Optional.of(user));

        assertThrows(DuplicateResourceException.class, () -> employeeService.create(request));
    }

    @Test
    void create_lanzaExcepcion_cuandoSucursalNoExiste() {
        EmployeeRequest request = new EmployeeRequest(
                "empleado@example.com", "password123", "Ana", "Gomez",
                UserRole.BARISTA, branchId, "Barista", LocalDate.of(2022, 3, 1));
        when(userRepository.findByEmailIgnoreCase("empleado@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.create(request));
    }

    @Test
    void update_actualizaEmployeeYSincronizaRolDeUser_cuandoExiste() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(UserRole.MANAGER, branchId, "Supervisor", LocalDate.of(2023, 6, 15));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse result = employeeService.update(employeeId, request);

        assertThat(result.position()).isEqualTo("Supervisor");
        assertThat(result.role()).isEqualTo("MANAGER");
        assertThat(user.getRole()).isEqualTo(UserRole.MANAGER);
        verify(employeeRepository).save(employee);
        verify(userRepository).save(user);
    }

    @Test
    void update_lanzaExcepcion_cuandoTypeEsCustomer() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(UserRole.CUSTOMER, branchId, "Supervisor", LocalDate.of(2023, 6, 15));

        assertThrows(InvalidStateException.class, () -> employeeService.update(employeeId, request));
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(UserRole.MANAGER, branchId, "Supervisor", LocalDate.of(2023, 6, 15));
        when(employeeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.update(missingId, request));
    }

    @Test
    void delete_marcaEmployeeComoInactivo() {
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        employeeService.delete(employeeId);

        assertThat(employee.getActive()).isFalse();
        verify(employeeRepository).save(employee);
    }
}
