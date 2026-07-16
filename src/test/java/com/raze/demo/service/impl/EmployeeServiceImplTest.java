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
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.Branch;
import com.raze.demo.model.Employee;
import com.raze.demo.model.User;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.repository.EmployeeRepository;
import com.raze.demo.repository.UserRepository;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link CustomerServiceImplTest} y
 * {@link BranchServiceImplTest}: Mockito puro, sin Spring, sin base de datos.
 *
 * EmployeeServiceImpl depende de TRES repositorios (Employee, User, Branch), así que hay
 * tres @Mock. @InjectMocks los combina todos al construir el service real, en el orden
 * que espera su constructor (generado por @RequiredArgsConstructor).
 */
@ExtendWith(MockitoExtension.class)
// Extensión de JUnit 5 que inicializa los mocks (@Mock) y arma el objeto bajo prueba
// (@InjectMocks) antes de cada test. Es puramente un mecanismo de Mockito, no involucra
// a Spring en ningún momento.
class EmployeeServiceImplTest {

    @Mock
    // Repositorio "falso" del propio Employee: sin esto no podríamos simular
    // "ya existe" / "no existe" sin tocar una base de datos real.
    private EmployeeRepository employeeRepository;

    @Mock
    // Se usa para simular la búsqueda del User al que se le crea el perfil de empleado.
    private UserRepository userRepository;

    @Mock
    // Se usa para simular la búsqueda de la sucursal (Branch) a la que se asigna el empleado.
    private BranchRepository branchRepository;

    @InjectMocks
    // Instancia real de EmployeeServiceImpl con los tres mocks de arriba inyectados.
    // Esto es lo que realmente estamos probando: su lógica (validaciones, orquestación),
    // no los repositorios (esos están falseados a propósito).
    private EmployeeServiceImpl employeeService;

    private UUID employeeId;
    private UUID branchId;
    private User user;
    private Branch branch;
    private Employee employee;

    @BeforeEach
    // Corre antes de cada @Test para que ningún test reutilice (o corrompa) el estado
    // dejado por otro. Aquí se arma el "mundo" mínimo: un User, una Branch y un Employee
    // que los conecta a ambos.
    void setUp() {
        employeeId = UUID.randomUUID();
        branchId = UUID.randomUUID();

        user = new User();
        user.setId(employeeId);
        user.setEmail("empleado@example.com");
        user.setFirstName("Ana");
        user.setLastName("Gomez");

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
        employee.setRole("STAFF");
        employee.setHireDate(LocalDate.of(2022, 3, 1));
        employee.setActive(true);
    }

    @Test
    // Solo necesita: el mock devolviendo datos de prueba (Arrange), invocar el método
    // real del service (Act), y comprobar el resultado + la interacción (Assert).
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
    // Aquí se configuran los TRES mocks porque create() del service consulta los
    // tres repositorios en secuencia (existe empleado, existe user, existe branch)
    // antes de guardar.
    void create_guardaYRetornaEmployee_cuandoUsuarioYSucursalExisten() {
        EmployeeRequest request = new EmployeeRequest(employeeId, branchId, "Barista", "STAFF", LocalDate.of(2022, 3, 1));
        when(employeeRepository.existsById(employeeId)).thenReturn(false);
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(user));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse result = employeeService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(employeeId);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void create_lanzaExcepcion_cuandoUsuarioYaTieneEmployee() {
        EmployeeRequest request = new EmployeeRequest(employeeId, branchId, "Barista", "STAFF", LocalDate.of(2022, 3, 1));
        when(employeeRepository.existsById(employeeId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.create(request));
    }

    @Test
    void create_lanzaExcepcion_cuandoUsuarioNoExiste() {
        EmployeeRequest request = new EmployeeRequest(employeeId, branchId, "Barista", "STAFF", LocalDate.of(2022, 3, 1));
        when(employeeRepository.existsById(employeeId)).thenReturn(false);
        when(userRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.create(request));
    }

    @Test
    // Nótese que aquí SÍ se configura userRepository (encuentra al user) pero NO
    // branchRepository (queda con su comportamiento por defecto de Mockito: devolver
    // Optional.empty()), para forzar justo la rama de "sucursal no existe".
    void create_lanzaExcepcion_cuandoSucursalNoExiste() {
        EmployeeRequest request = new EmployeeRequest(employeeId, branchId, "Barista", "STAFF", LocalDate.of(2022, 3, 1));
        when(employeeRepository.existsById(employeeId)).thenReturn(false);
        when(userRepository.findById(employeeId)).thenReturn(Optional.of(user));
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.create(request));
    }

    @Test
    void update_actualizaYRetornaEmployee_cuandoExiste() {
        EmployeeRequest request = new EmployeeRequest(employeeId, branchId, "Supervisor", "MANAGER", LocalDate.of(2023, 6, 15));
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse result = employeeService.update(employeeId, request);

        assertThat(result.position()).isEqualTo("Supervisor");
        assertThat(result.role()).isEqualTo("MANAGER");
        verify(employeeRepository).save(employee);
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        EmployeeRequest request = new EmployeeRequest(missingId, branchId, "Supervisor", "MANAGER", LocalDate.of(2023, 6, 15));
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
