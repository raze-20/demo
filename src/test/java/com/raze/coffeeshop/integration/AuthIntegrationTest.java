package com.raze.coffeeshop.integration;

import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.EmployeeRepository;
import com.raze.coffeeshop.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — prueba el flujo real de autenticación y autorización por
 * rol contra Postgres real: login emite un JWT firmado, y ese JWT se usa en el header
 * Authorization de requests posteriores contra endpoints protegidos reales (no mockeados).
 *
 * Los usuarios de prueba se siembran directamente vía los repositorios (mismo patrón que
 * {@link OrderConcurrencyIntegrationTest}) porque, con seguridad ya activa, los propios
 * endpoints de alta (`/api/employees`) ahora requieren un token ADMIN/MANAGER — no se puede
 * usar el endpoint para crear al primer usuario administrador de la prueba.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String seedStaffUser(String email, String rawPassword, UserRole role, Branch branch) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName("Test");
        user.setLastName(role.name());
        user.setRole(role);
        user = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setBranch(branch);
        employee.setPosition(role.name());
        employee.setRole(role.name());
        employee.setHireDate(LocalDate.now());
        employeeRepository.save(employee);

        return email;
    }

    private String login(String email, String password) throws Exception {
        String loginJson = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asString();
    }

    @Test
    void flujoCompleto_loginYAutorizacionPorRol() throws Exception {
        Branch branch = new Branch();
        branch.setName("Sucursal Auth");
        branch.setAddress("Av. Segura 100");
        branch.setCity("Guadalajara");
        branch.setState("JAL");
        branch = branchRepository.save(branch);

        String adminEmail = seedStaffUser("admin.auth@test.com", "AdminPass123", UserRole.ADMIN, branch);
        String baristaEmail = seedStaffUser("barista.auth@test.com", "BaristaPass123", UserRole.BARISTA, branch);

        // Sin token, un endpoint protegido responde 401 (no el error por defecto de Spring).
        mockMvc.perform(get("/api/v1/branches"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // La documentación OpenAPI es publica (permitida en SecurityConfig): 200 sin token.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        // Login con password incorrecta responde 401.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin.auth@test.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized());

        String adminToken = login(adminEmail, "AdminPass123");
        String baristaToken = login(baristaEmail, "BaristaPass123");

        // Cualquier rol autenticado puede leer.
        mockMvc.perform(get("/api/v1/branches").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/branches").header("Authorization", "Bearer " + baristaToken))
                .andExpect(status().isOk());

        String newBranchJson = """
                {"name":"Sucursal Nueva","address":"Calle 1","city":"Monterrey","state":"NL"}
                """;

        // BARISTA no puede crear sucursales (solo ADMIN/MANAGER): 403, no un 500 ni un 401.
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + baristaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBranchJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // ADMIN sí puede.
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newBranchJson))
                .andExpect(status().isCreated());

        // Un token con firma inválida se rechaza igual que no mandar ninguno.
        mockMvc.perform(get("/api/v1/branches").header("Authorization", "Bearer not.a.valid.token"))
                .andExpect(status().isUnauthorized());
    }
}
