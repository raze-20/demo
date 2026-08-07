package com.raze.coffeeshop.integration;

import com.raze.coffeeshop.enums.OrderStatus;
import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.Order;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.EmployeeRepository;
import com.raze.coffeeshop.repository.OrderRepository;
import com.raze.coffeeshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TEST DE INTEGRACIÓN COMPLETO — prueba el mecanismo real de bloqueo optimista (columna
 * {@code version} en {@link Order}) contra Postgres real, simulando de forma determinística
 * (sin depender del timing de hilos) la condición de carrera que motivó agregarlo: dos
 * "solicitudes" que leen la misma orden con la misma versión, donde la primera confirma su
 * cambio y la segunda intenta confirmar el suyo sobre una versión ya obsoleta. Esto reemplaza
 * una prueba con hilos reales (propensa a fallar por timing) por una que fuerza el mismo
 * escenario de forma reproducible.
 */
@Testcontainers
@SpringBootTest
class OrderConcurrencyIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void segundaEscrituraConVersionObsoleta_lanzaExcepcionDeBloqueoOptimista() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        UUID orderId = tx.execute(status -> {
            Branch branch = new Branch();
            branch.setName("Sucursal Concurrencia");
            branch.setAddress("Calle Falsa 123");
            branch.setCity("Guadalajara");
            branch.setState("JAL");
            branch = branchRepository.save(branch);

            User user = new User();
            user.setEmail("concurrencia@test.com");
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setFirstName("Con");
            user.setLastName("Currencia");
            user.setRole(UserRole.CASHIER);
            user = userRepository.save(user);

            Employee employee = new Employee();
            employee.setUser(user);
            employee.setBranch(branch);
            employee.setPosition("Cajera");
            employee.setRole(UserRole.CASHIER.name());
            employee.setHireDate(LocalDate.now());
            employee = employeeRepository.save(employee);

            Order order = new Order();
            order.setBranch(branch);
            order.setEmployee(employee);
            order.setTotal(new BigDecimal("100.00"));
            order = orderRepository.save(order);
            return order.getId();
        });

        // Dos "solicitudes" leen la misma orden, en la misma versión, en transacciones separadas.
        Order requestASnapshot = tx.execute(status -> orderRepository.findById(orderId).orElseThrow());
        Order requestBSnapshot = tx.execute(status -> orderRepository.findById(orderId).orElseThrow());

        // La solicitud A confirma primero: la versión de la orden avanza.
        tx.executeWithoutResult(status -> {
            requestASnapshot.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(requestASnapshot);
        });

        // La solicitud B todavía tiene la versión vieja: su escritura debe ser rechazada,
        // no aplicada silenciosamente encima del cambio de A.
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> tx.executeWithoutResult(status -> {
            requestBSnapshot.setStatus(OrderStatus.PREPARING);
            orderRepository.saveAndFlush(requestBSnapshot);
        }));
    }
}
