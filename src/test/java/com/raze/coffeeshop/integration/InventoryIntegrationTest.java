package com.raze.coffeeshop.integration;

import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.model.Category;
import com.raze.coffeeshop.model.Employee;
import com.raze.coffeeshop.model.Ingredient;
import com.raze.coffeeshop.model.Product;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.BranchRepository;
import com.raze.coffeeshop.repository.CategoryRepository;
import com.raze.coffeeshop.repository.EmployeeRepository;
import com.raze.coffeeshop.repository.IngredientRepository;
import com.raze.coffeeshop.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — cubre el flujo de inventario de punta a punta contra Postgres
 * real, usando tokens JWT reales (login) para ejercitar también la autorización por rol y el
 * resolver de {@code @AuthenticationPrincipal}, que un slice {@code @WebMvcTest} no activa:
 * definir la receta de un producto, stockear la sucursal con un movimiento INCOMING, vender el
 * producto vía {@code /api/orders} (lo que descuenta los ingredientes y registra un movimiento
 * SALE), y confirmar que un segundo pedido sin stock suficiente se rechaza.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InventoryIntegrationTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID seedEmployee(String email, String rawPassword, UserRole role, Branch branch) {
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

        return user.getId();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void flujoCompleto_recetaStockVentaDescuentoYFaltante() throws Exception {
        Branch branch = new Branch();
        branch.setName("Sucursal Inventario");
        branch.setAddress("Av. Stock 100");
        branch.setCity("Guadalajara");
        branch.setState("JAL");
        branch = branchRepository.save(branch);

        Category category = new Category();
        category.setName("Cafe caliente");
        category.setActive(true);
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Latte");
        product.setBasePrice(new BigDecimal("50.00"));
        product.setActive(true);
        product.setCategory(category);
        product = productRepository.save(product);
        UUID productId = product.getId();

        Ingredient milk = new Ingredient();
        milk.setName("Leche entera");
        milk.setMeasureUnit("ml");
        milk.setActive(true);
        milk = ingredientRepository.save(milk);
        UUID ingredientId = milk.getId();

        UUID adminUserId = seedEmployee("admin.inv@test.com", "AdminPass123", UserRole.ADMIN, branch);
        seedEmployee("barista.inv@test.com", "BaristaPass123", UserRole.BARISTA, branch);

        String adminToken = login("admin.inv@test.com", "AdminPass123");
        String baristaToken = login("barista.inv@test.com", "BaristaPass123");
        UUID branchId = branch.getId();

        // Definir la receta: cada Latte requiere 150 ml de leche.
        mockMvc.perform(post("/api/v1/products/" + productId + "/recipes")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientId":"%s","requiredQuantity":150.000}
                                """.formatted(ingredientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredientName").value("Leche entera"));

        // Un BARISTA no puede registrar movimientos de inventario (solo ADMIN/MANAGER): 403.
        mockMvc.perform(post("/api/v1/branches/" + branchId + "/inventory/movements")
                        .header("Authorization", bearer(baristaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientId":"%s","type":"INCOMING","quantity":500.000}
                                """.formatted(ingredientId)))
                .andExpect(status().isForbidden());

        // El ADMIN stockea 500 ml de leche (movimiento INCOMING).
        mockMvc.perform(post("/api/v1/branches/" + branchId + "/inventory/movements")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientId":"%s","type":"INCOMING","quantity":500.000,"reason":"Compra semanal"}
                                """.formatted(ingredientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INCOMING"))
                .andExpect(jsonPath("$.performedByUserId").value(adminUserId.toString()));

        // Registrar un movimiento SALE manualmente está prohibido (solo lo dispara la venta).
        mockMvc.perform(post("/api/v1/branches/" + branchId + "/inventory/movements")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ingredientId":"%s","type":"SALE","quantity":10.000}
                                """.formatted(ingredientId)))
                .andExpect(status().isBadRequest());

        // Confirmar el stock inicial: 500 ml.
        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentQuantity").value(500.000));

        // Vender 2 Lattes: crea orden, agrega item, y paga el total (100 + 16% IVA = 116.00)
        // → descuenta 2 * 150 = 300 ml.
        String orderId = createPaidOrder(branchId, adminUserId, productId, adminToken, 2, "116.00");

        // El stock quedó en 500 - 300 = 200 ml.
        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentQuantity").value(200.000));

        // El historial del ingrediente incluye el movimiento SALE de la venta.
        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory/" + ingredientId + "/movements")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'SALE')].quantity").value(org.hamcrest.Matchers.hasItem(300.0)))
                .andExpect(jsonPath("$[?(@.type == 'SALE')].reason").value(org.hamcrest.Matchers.hasItem("Order " + orderId)));

        // Segundo pedido de 2 Lattes: necesita 300 ml pero solo quedan 200 → el pago se rechaza
        // (400) y, al revertirse la transacción, el stock no cambia.
        String secondOrderId = createOrderWithItem(branchId, adminUserId, productId, adminToken, 2);
        mockMvc.perform(post("/api/v1/orders/" + secondOrderId + "/payments")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"CASH","amount":116.00}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/branches/" + branchId + "/inventory")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentQuantity").value(200.000));
    }

    private String createOrderWithItem(UUID branchId, UUID employeeId, UUID productId, String token, int quantity) throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":"%s","employeeId":"%s"}
                                """.formatted(branchId, employeeId)))
                .andExpect(status().isCreated())
                .andReturn();
        String orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asString();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":%d}
                                """.formatted(productId, quantity)))
                .andExpect(status().isCreated());

        return orderId;
    }

    private String createPaidOrder(UUID branchId, UUID employeeId, UUID productId, String token, int quantity, String amount) throws Exception {
        String orderId = createOrderWithItem(branchId, employeeId, productId, token, quantity);

        MvcResult paid = mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method":"CASH","amount":%s}
                                """.formatted(amount)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(paid.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.get("status").asString()).isEqualTo("PAID");

        return orderId;
    }
}
