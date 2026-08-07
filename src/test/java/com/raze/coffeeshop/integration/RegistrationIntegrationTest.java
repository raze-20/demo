package com.raze.coffeeshop.integration;

import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Customer;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.CustomerRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — mismo patrón que {@link AuthIntegrationTest}: contexto de
 * Spring completo, Postgres real en un contenedor de Testcontainers, Flyway aplicado y tokens
 * JWT reales emitidos por {@code /api/v1/auth/login}.
 *
 * Cubre el alta en un solo paso de {@code customers} y {@code employees}, que es justo lo que
 * los tests con mocks no pueden probar: que el {@code User} y su perfil se escriben en la
 * MISMA transacción contra Postgres (y que si algo falla a la mitad no queda un usuario
 * huérfano), que el {@code user.role} que se guarda coincide con el perfil creado, y que la
 * cuenta recién dada de alta sirve de verdad para hacer login y recibe exactamente los
 * permisos de su rol.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RegistrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------- customers

    @Test
    void autoRegistroDeCliente_creaUsuarioYPerfilEnUnSoloPaso() throws Exception {
        String email = "cliente-" + UUID.randomUUID() + "@example.com";
        String json = """
                {"email":"%s","password":"SuperSecreta123","firstName":"Ana","lastName":"Lopez","birthDate":"1998-05-12"}
                """.formatted(email);

        // POST SIN token: el auto-registro de clientes es el único endpoint de alta público
        // (declarado en PublicEndpoints). Si dejara de serlo, este 201 se volvería 401.
        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value(email))
                // loyaltyPoints es opcional en el request: el service lo normaliza a 0.
                .andExpect(jsonPath("$.loyaltyPoints").value(0))
                // La respuesta del perfil nunca devuelve credenciales.
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        UUID userId = UUID.fromString(readField(result, "userId"));

        // Las dos filas reales en Postgres: el user y su perfil, con la MISMA PK (@MapsId).
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(user.getPasswordHash()).isNotEqualTo("SuperSecreta123").startsWith("$2");

        Customer customer = customerRepository.findById(userId).orElseThrow();
        assertThat(customer.getUserId()).isEqualTo(userId);
        assertThat(customer.getBirthDate()).isEqualTo(LocalDate.of(1998, 5, 12));
        assertThat(customer.getActive()).isTrue();

        // La cuenta recién creada sirve de verdad para entrar: el hash que guardó el alta lo
        // valida el AuthenticationManager real en el login.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"SuperSecreta123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void autoRegistroDeCliente_ignoraUnRolInyectadoEnElJson() throws Exception {
        String email = "escalada-" + UUID.randomUUID() + "@example.com";

        // CustomerRequest no tiene campo `role`: mandarlo no debe escalar privilegios. Es una
        // prueba de la superficie HTTP real (deserialización incluida), no del service.
        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"SuperSecreta123","firstName":"Eva","lastName":"Cruz","role":"ADMIN"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID userId = UUID.fromString(readField(result, "userId"));
        assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void autoRegistroDeCliente_retorna409_cuandoCorreoYaRegistrado() throws Exception {
        String email = "cliente-dup-" + UUID.randomUUID() + "@example.com";
        String json = """
                {"email":"%s","password":"SuperSecreta123","firstName":"Ana","lastName":"Lopez"}
                """.formatted(email);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        long usersAfterFirst = userRepository.count();
        long customersAfterFirst = customerRepository.count();

        // El duplicado lo detecta el service contra Postgres (no contra un mock) y responde
        // 409, antes de que la restricción UNIQUE de la tabla users reviente con un 500.
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Y el intento rechazado no escribió nada: ni un user ni un perfil a medias.
        assertThat(userRepository.count()).isEqualTo(usersAfterFirst);
        assertThat(customerRepository.count()).isEqualTo(customersAfterFirst);
    }

    @Test
    void clienteRegistrado_soloAccedeASuPropioPerfil() throws Exception {
        String ownEmail = "duena-" + UUID.randomUUID() + "@example.com";
        String otherEmail = "ajena-" + UUID.randomUUID() + "@example.com";

        UUID ownId = registerCustomer(ownEmail);
        UUID otherId = registerCustomer(otherEmail);

        String token = login(ownEmail, "SuperSecreta123");

        // La regla de "dueño del recurso" (#userId == authentication.principal.id) necesita el
        // principal real que arma el filtro JWT: un slice de controller no la ejercita.
        mockMvc.perform(get("/api/v1/customers/" + ownId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ownEmail));

        // El perfil de otro cliente es ajeno: 403, no 200 ni 404.
        mockMvc.perform(get("/api/v1/customers/" + otherId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // Y el listado completo es solo para ADMIN/MANAGER.
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- employees

    @Test
    void altaDeEmpleado_creaUsuarioConElRolOperativoIndicado() throws Exception {
        Branch branch = seedBranch("Sucursal Alta");
        String adminEmail = seedStaffUser("admin.alta@test.com", UserRole.ADMIN, branch);
        String adminToken = login(adminEmail, STAFF_PASSWORD);

        String email = "barista-" + UUID.randomUUID() + "@example.com";
        String json = employeeJson(email, "BARISTA", branch.getId());

        // Sin token no se puede dar de alta personal (a diferencia de /customers, este
        // endpoint no es público).
        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());

        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("BARISTA"))
                // El join real con branches resuelve el nombre; un mock lo "adivinaría".
                .andExpect(jsonPath("$.branchId").value(branch.getId().toString()))
                .andExpect(jsonPath("$.branchName").value("Sucursal Alta"))
                .andReturn();

        UUID userId = UUID.fromString(readField(result, "userId"));

        // La invariante del alta en un solo paso: user.role y employee.role no pueden quedar
        // desalineados porque los escribe la misma transacción.
        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.BARISTA);
        assertThat(user.getPasswordHash()).startsWith("$2");

        Employee employee = employeeRepository.findById(userId).orElseThrow();
        assertThat(employee.getRole()).isEqualTo("BARISTA");
        assertThat(employee.getBranch().getId()).isEqualTo(branch.getId());
        assertThat(employee.getActive()).isTrue();

        // El empleado nuevo entra con sus credenciales y recibe exactamente los permisos de
        // BARISTA: puede leer el catálogo, pero no escribirlo.
        String baristaToken = login(email, "SuperSecreta123");
        mockMvc.perform(get("/api/v1/branches").header("Authorization", "Bearer " + baristaToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + baristaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("otro-" + UUID.randomUUID() + "@example.com", "BARISTA", branch.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void reasignarTipoDeEmpleado_sincronizaElRolDelUsuarioYSusPermisos() throws Exception {
        Branch branch = seedBranch("Sucursal Ascenso");
        String adminToken = login(seedStaffUser("admin.ascenso@test.com", UserRole.ADMIN, branch), STAFF_PASSWORD);

        String email = "ascenso-" + UUID.randomUUID() + "@example.com";
        MvcResult created = mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(email, "BARISTA", branch.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID userId = UUID.fromString(readField(created, "userId"));

        String branchJson = """
                {"name":"Sucursal Nueva","address":"Calle 1","city":"Monterrey","state":"NL"}
                """;

        // Como BARISTA no puede crear sucursales.
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + login(email, "SuperSecreta123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson))
                .andExpect(status().isForbidden());

        // El ADMIN lo promueve a MANAGER.
        mockMvc.perform(put("/api/v1/employees/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"MANAGER","branchId":"%s","position":"Gerente","hireDate":"2024-01-15"}
                                """.formatted(branch.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        // El PUT tiene que haber tocado también la fila de users, no solo la de employees:
        // si no, el login seguiría emitiendo un token de BARISTA.
        assertThat(userRepository.findById(userId).orElseThrow().getRole()).isEqualTo(UserRole.MANAGER);

        String managerToken = login(email, "SuperSecreta123");
        mockMvc.perform(post("/api/v1/branches")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson))
                .andExpect(status().isCreated());
    }

    @Test
    void altaDeEmpleado_retorna400_cuandoElTipoEsCustomer() throws Exception {
        Branch branch = seedBranch("Sucursal Tipo");
        String adminToken = login(seedStaffUser("admin.tipo@test.com", UserRole.ADMIN, branch), STAFF_PASSWORD);

        String email = "tipo-invalido-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(email, "CUSTOMER", branch.getId())))
                .andExpect(status().isBadRequest());

        // El rechazo ocurre antes de escribir nada.
        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void altaDeEmpleado_conSucursalInexistente_revierteYNoDejaUsuarioHuerfano() throws Exception {
        Branch branch = seedBranch("Sucursal Rollback");
        String adminToken = login(seedStaffUser("admin.rollback@test.com", UserRole.ADMIN, branch), STAFF_PASSWORD);

        String email = "huerfano-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(email, "BARISTA", UUID.randomUUID())))
                .andExpect(status().isNotFound());

        // Esta es la assertion que solo puede hacer un test de integración: el service guarda
        // el User ANTES de resolver la sucursal, así que el 404 tiene que revertir esa
        // escritura. Si el alta no fuera @Transactional, aquí quedaría un usuario sin perfil
        // ocupando el correo para siempre.
        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void altaDeEmpleado_retorna409_cuandoCorreoYaRegistrado() throws Exception {
        Branch branch = seedBranch("Sucursal Duplicado");
        String adminToken = login(seedStaffUser("admin.dup@test.com", UserRole.ADMIN, branch), STAFF_PASSWORD);

        String email = "empleado-dup-" + UUID.randomUUID() + "@example.com";
        String json = employeeJson(email, "CASHIER", branch.getId());

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void correoYaUsadoPorUnCliente_bloqueaElAltaDeEmpleado() throws Exception {
        Branch branch = seedBranch("Sucursal Cruzada");
        String adminToken = login(seedStaffUser("admin.cruzado@test.com", UserRole.ADMIN, branch), STAFF_PASSWORD);

        String email = "cruzado-" + UUID.randomUUID() + "@example.com";
        registerCustomer(email);

        // Los dos endpoints de alta comparten la tabla users: un correo tomado por un cliente
        // no puede reutilizarse para un empleado (el correo es la credencial de login).
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(email, "CASHIER", branch.getId())))
                .andExpect(status().isConflict());

        assertThat(userRepository.findByEmailIgnoreCase(email).orElseThrow().getRole())
                .isEqualTo(UserRole.CUSTOMER);
    }

    // ---------------------------------------------------------------- helpers

    private static final String STAFF_PASSWORD = "StaffPass123";

    private Branch seedBranch(String name) {
        Branch branch = new Branch();
        branch.setName(name);
        branch.setAddress("Av. Registro 100");
        branch.setCity("Guadalajara");
        branch.setState("JAL");
        return branchRepository.save(branch);
    }

    /**
     * Siembra un empleado directo por repositorio: con la seguridad activa, el endpoint de
     * alta ya exige un token ADMIN/MANAGER, así que el primer administrador no puede crearse
     * a través de la propia API (mismo patrón que {@link AuthIntegrationTest}).
     */
    private String seedStaffUser(String email, UserRole role, Branch branch) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(STAFF_PASSWORD));
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

    private UUID registerCustomer(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"SuperSecreta123","firstName":"Ana","lastName":"Lopez"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(readField(result, "userId"));
    }

    private String employeeJson(String email, String type, UUID branchId) {
        return """
                {"email":"%s","password":"SuperSecreta123","firstName":"Carlos","lastName":"Ruiz","type":"%s","branchId":"%s","position":"Barista","hireDate":"2024-01-15"}
                """.formatted(email, type, branchId);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readField(result, "token");
    }

    private String readField(MvcResult result, String field) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get(field).asString();
    }
}
