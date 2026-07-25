package com.raze.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — mismo patrón que {@link BranchIntegrationTest}: contexto de
 * Spring completo, Postgres real en un contenedor de Testcontainers, Flyway aplicado.
 *
 * Cubre el flujo crítico del catálogo: crear una categoría, crear un producto que la
 * referencia y confirmar que la relación (categoryId/categoryName) sobrevive el viaje real
 * por Hibernate/Postgres. Un mock de CategoryRepository jamás detectaría, por ejemplo, un
 * @ManyToOne mal mapeado o una FK que no cuadra con el esquema de Flyway.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void flujoCompleto_crearCategoriaYProductoAsociado() throws Exception {
        String categoryJson = """
                {"name":"Cafe caliente","active":true}
                """;

        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode categoryBody = objectMapper.readTree(categoryResult.getResponse().getContentAsString());
        int categoryId = categoryBody.get("id").asInt();

        String productJson = """
                {"name":"Latte","basePrice":55.00,"active":true,"categoryId":%d}
                """.formatted(categoryId);

        // POST real: pasa por el controller, el service (que valida que la categoría exista
        // vía CategoryRepository real) y el INSERT real contra Postgres.
        mockMvc.perform(post("/api/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Cafe caliente"));

        // GET real: confirma que el producto recién creado aparece en el listado con la
        // relación a su categoría ya resuelta (join real, no un mock que "adivina" el nombre).
        mockMvc.perform(get("/api/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Latte"))
                .andExpect(jsonPath("$[0].categoryName").value("Cafe caliente"));
    }

    @Test
    // El service real debe rechazar un categoryId que no existe en Postgres con 404, no con
    // un error de FK constraint sin manejar.
    void create_retorna404_cuandoCategoriaNoExiste() throws Exception {
        String productJson = """
                {"name":"Producto huerfano","basePrice":10.00,"active":true,"categoryId":999999}
                """;

        mockMvc.perform(post("/api/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isNotFound());
    }
}
