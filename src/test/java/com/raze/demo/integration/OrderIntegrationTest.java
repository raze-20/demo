package com.raze.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — mismo patrón que {@link ProductIntegrationTest}: contexto de
 * Spring completo, Postgres real en un contenedor de Testcontainers, Flyway aplicado.
 *
 * Cubre el flujo de ventas de punta a punta: crear orden, agregar items (congelando el precio
 * vigente del producto en ese momento), registrar pagos parciales hasta cubrir el total
 * (con transición automática a PAID) y avanzar el estado operativo de la orden. Un test con
 * mocks nunca hubiera detectado, por ejemplo, que el precio del item sobrevive un cambio
 * posterior del precio del producto en la base real.
 *
 * Cada request usa {@code .with(admin())} para simular un ADMIN autenticado (el endpoint ya
 * exige auth real); el login real de punta a punta se cubre en {@link AuthIntegrationTest}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    @Test
    void flujoCompleto_ordenItemsPagosYCambiosDeEstado() throws Exception {
        String branchJson = """
                {"name":"Sucursal Centro","address":"Av. Real 100","city":"Guadalajara","state":"JAL"}
                """;
        MvcResult branchResult = mockMvc.perform(post("/api/v1/branches")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(branchJson))
                .andExpect(status().isCreated())
                .andReturn();
        String branchId = readField(branchResult, "id");

        String categoryJson = """
                {"name":"Cafe caliente","active":true}
                """;
        MvcResult categoryResult = mockMvc.perform(post("/api/v1/categories")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isCreated())
                .andReturn();
        int categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString()).get("id").asInt();

        String productJson = """
                {"name":"Latte","basePrice":55.00,"active":true,"categoryId":%d}
                """.formatted(categoryId);
        MvcResult productResult = mockMvc.perform(post("/api/v1/products")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andReturn();
        String productId = readField(productResult, "id");

        String employeeJson = """
                {"email":"cajero.orden@test.com","password":"password123","firstName":"Ana","lastName":"Ramirez","type":"CASHIER","branchId":"%s","position":"Cajera","hireDate":"2024-01-15"}
                """.formatted(branchId);
        MvcResult employeeResult = mockMvc.perform(post("/api/v1/employees")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson))
                .andExpect(status().isCreated())
                .andReturn();
        String employeeId = objectMapper.readTree(employeeResult.getResponse().getContentAsString())
                .get("userId").asText();

        String orderJson = """
                {"branchId":"%s","employeeId":"%s"}
                """.formatted(branchId, employeeId);
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        String orderId = readField(orderResult, "id");

        // Agrega 2 unidades al precio vigente (55.00): subtotal 110.00, taxes 17.60 (16%), total 127.60.
        String itemJson = """
                {"productId":"%s","quantity":2}
                """.formatted(productId);
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/items")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(110.00))
                .andExpect(jsonPath("$.taxes").value(17.60))
                .andExpect(jsonPath("$.total").value(127.60));

        // El precio del producto sube DESPUÉS de que el item ya fue vendido.
        String updatedProductJson = """
                {"name":"Latte","basePrice":90.00,"active":true,"categoryId":%d}
                """.formatted(categoryId);
        mockMvc.perform(put("/api/v1/products/" + productId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedProductJson))
                .andExpect(status().isOk());

        // El item ya vendido conserva el precio original (55.00), no el nuevo (90.00), y el
        // total de la orden no se recalcula con el precio nuevo.
        mockMvc.perform(get("/api/v1/orders/" + orderId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(55.00))
                .andExpect(jsonPath("$.total").value(127.60));

        // Pago parcial en efectivo: la orden sigue PENDING con saldo pendiente.
        String firstPaymentJson = """
                {"method":"CASH","amount":100.00}
                """;
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstPaymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.balanceDue").value(27.60));

        // Segundo pago (con otro método) que completa el total: transición automática a PAID.
        String secondPaymentJson = """
                {"method":"CARD","amount":27.60}
                """;
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPaymentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.balanceDue").value(0.00));

        // Ya no se pueden agregar items a una orden que dejó de estar PENDING.
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/items")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isBadRequest());

        // El staff avanza el estado operativo de la orden ya pagada.
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PREPARING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARING"));

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // DELIVERED es un estado terminal: no puede retroceder a PREPARING.
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"PREPARING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    // El service real debe rechazar un branchId que no existe en Postgres con 404.
    void create_retorna404_cuandoBranchNoExiste() throws Exception {
        String orderJson = """
                {"branchId":"%s","employeeId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/v1/orders")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isNotFound());
    }

    private String readField(MvcResult result, String field) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get(field).asText();
    }
}
