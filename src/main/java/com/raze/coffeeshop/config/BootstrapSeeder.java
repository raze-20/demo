package com.raze.coffeeshop.config;

import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.EmployeeRepository;
import com.raze.coffeeshop.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * Siembra el primer empleado con acceso al sistema. Sin el, una base recien migrada por Flyway
 * queda vacia y no hay forma de entrar: {@code POST /api/v1/auth/login} necesita un {@code user}
 * ya existente y el resto de la API exige token (solo el alta de clientes es publica, y un
 * {@code CUSTOMER} no puede operar el front de personal). Ademas el punto de venta manda el uid
 * de la sesion como {@code OrderRequest.employeeId}, asi que la primera cuenta tiene que ser un
 * empleado con sucursal, no un usuario ADMIN suelto.
 *
 * <p>No depende del perfil sino de {@code app.seed.enabled}, que en {@code dev} viene encendido
 * y en cualquier otro entorno hay que pedir a proposito. El arranque en frio de produccion se
 * resuelve encendiendolo para el primer despliegue y apagandolo despues de cambiar la clave;
 * asi las credenciales iniciales viajan por variables de entorno y no quedan versionadas en una
 * migracion de Flyway.
 *
 * <p>Es idempotente: si ya existe un usuario con el correo configurado no toca nada, asi que
 * puede quedarse encendido entre reinicios sin duplicar datos ni pisar una clave cambiada
 * despues. Los valores se configuran bajo {@code app.seed.*}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BootstrapSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.employee.email:}")
    private String email;

    @Value("${app.seed.employee.password:}")
    private String password;

    @Value("${app.seed.employee.first-name}")
    private String firstName;

    @Value("${app.seed.employee.last-name}")
    private String lastName;

    @Value("${app.seed.employee.position}")
    private String position;

    @Value("${app.seed.branch.name}")
    private String branchName;

    @Value("${app.seed.branch.address}")
    private String branchAddress;

    @Value("${app.seed.branch.city}")
    private String branchCity;

    @Value("${app.seed.branch.state}")
    private String branchState;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Correo y clave son los unicos valores sin default: el resto describe a la sucursal de
        // arranque y puede caer en un generico, pero sembrar una cuenta con credenciales vacias
        // dejaria la API abierta. Mejor no arrancar que arrancar con un acceso adivinable.
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "app.seed.enabled=true pero falta app.seed.employee.email o .password "
                            + "(APP_SEED_EMPLOYEE_EMAIL / APP_SEED_EMPLOYEE_PASSWORD)");
        }

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.debug("Bootstrap seed skipped: user {} already exists", email);
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(UserRole.ADMIN);
        user = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setBranch(seedBranch());
        employee.setPosition(position);
        employee.setRole(UserRole.ADMIN.name());
        employee.setHireDate(LocalDate.now());
        employeeRepository.save(employee);

        log.info("Bootstrap seed: ADMIN employee created (email={}). "
                + "Cambia la clave y apaga app.seed.enabled despues del primer acceso.", email);
    }

    /**
     * Reutiliza la primera sucursal existente (un empleado necesita una) y solo crea la de
     * arranque cuando la tabla esta vacia, para no ensuciar una base que ya tiene sucursales
     * reales.
     */
    private Branch seedBranch() {
        return branchRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Branch branch = new Branch();
                    branch.setName(branchName);
                    branch.setAddress(branchAddress);
                    branch.setCity(branchCity);
                    branch.setState(branchState);
                    log.info("Bootstrap seed: branch '{}' created", branchName);
                    return branchRepository.save(branch);
                });
    }
}
