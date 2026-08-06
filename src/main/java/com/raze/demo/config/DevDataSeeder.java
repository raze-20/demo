package com.raze.demo.config;

import com.raze.demo.enums.UserRole;
import com.raze.demo.model.Branch;
import com.raze.demo.model.Employee;
import com.raze.demo.model.User;
import com.raze.demo.repository.BranchRepository;
import com.raze.demo.repository.EmployeeRepository;
import com.raze.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Siembra un empleado de acceso para desarrollo. Sin el, una base recien migrada por Flyway
 * queda vacia y no hay forma de hacer login: {@code POST /api/v1/auth/login} necesita un
 * {@code user} ya existente y el resto de la API exige token (solo el alta de clientes es
 * publica, y un {@code CUSTOMER} no puede operar el front de personal).
 *
 * <p>Solo se activa con el perfil {@code dev} (el default local): en {@code prod} el alta del
 * primer administrador se hace a mano, para no dejar credenciales conocidas en produccion.
 *
 * <p>Es idempotente: si ya existe un usuario con el correo configurado no toca nada, asi que
 * puede correr en cada arranque sin duplicar datos ni pisar un password cambiado despues.
 * Los valores se configuran en {@code application-dev.yml} bajo {@code app.seed.*}.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.employee.email}")
    private String email;

    @Value("${app.seed.employee.password}")
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
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.debug("Dev seed skipped: user {} already exists", email);
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

        log.info("Dev seed: ADMIN employee created (email={})", email);
    }

    /**
     * Reutiliza la primera sucursal existente (un empleado necesita una) y solo crea la de
     * demo cuando la tabla esta vacia, para no ensuciar una base que ya tiene sucursales reales.
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
                    log.info("Dev seed: branch '{}' created", branchName);
                    return branchRepository.save(branch);
                });
    }
}
