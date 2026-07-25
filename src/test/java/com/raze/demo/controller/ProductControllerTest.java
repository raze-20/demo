package com.raze.demo.controller;

import com.raze.demo.dto.ProductRequest;
import com.raze.demo.dto.ProductResponse;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.security.JwtService;
import com.raze.demo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link BranchControllerTest}. Con Spring
 * Security en el classpath, el slice arma el filter chain real; @WithMockUser inyecta un
 * principal autenticado sin pasar por login/JWT real, y JwtService se mockea solo para que
 * el contexto pueda construir el SecurityFilterChain (no se invoca en estos tests).
 */
@WebMvcTest(ProductController.class)
@WithMockUser(roles = "ADMIN")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getById_retorna200_conProducto() throws Exception {
        UUID id = UUID.randomUUID();
        ProductResponse response = new ProductResponse(id, "Latte", new BigDecimal("55.00"), true, 1, "Coffee");
        when(productService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Latte"))
                .andExpect(jsonPath("$.categoryName").value("Coffee"));
    }

    @Test
    void getById_retorna404_cuandoNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenThrow(new ResourceNotFoundException("Product not found: " + id));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID id = UUID.randomUUID();
        ProductRequest request = new ProductRequest("Mocha", new BigDecimal("60.00"), true, 1);
        ProductResponse response = new ProductResponse(id, "Mocha", new BigDecimal("60.00"), true, 1, "Coffee");
        when(productService.create(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    // Bean Validation (@NotNull en basePrice y categoryId, @NotBlank en name) debe rechazar
    // la petición antes de tocar el service.
    void create_retorna400_cuandoFaltanCamposObligatorios() throws Exception {
        String invalidJson = """
                {"active":true}
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_retorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());
    }
}
