package com.raze.demo.controller;

import com.raze.demo.dto.RecipeRequest;
import com.raze.demo.dto.RecipeResponse;
import com.raze.demo.security.JwtService;
import com.raze.demo.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link ProductControllerTest}. Rol ADMIN por
 * defecto para satisfacer las reglas de escritura; el JwtService se mockea solo para poder
 * construir el SecurityFilterChain.
 */
@WebMvcTest(RecipeController.class)
@WithMockUser(roles = "ADMIN")
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getAll_retorna200_conLineasDeReceta() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        RecipeResponse response = new RecipeResponse(productId, ingredientId, "Leche entera", "ml", new BigDecimal("150.000"));
        when(recipeService.findByProduct(productId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products/" + productId + "/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ingredientName").value("Leche entera"));
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();
        RecipeRequest request = new RecipeRequest(ingredientId, new BigDecimal("150.000"));
        RecipeResponse response = new RecipeResponse(productId, ingredientId, "Leche entera", "ml", new BigDecimal("150.000"));
        when(recipeService.addToProduct(eq(productId), any(RecipeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products/" + productId + "/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredientId").value(ingredientId.toString()));
    }

    @Test
    // @NotNull en ingredientId y @DecimalMin en requiredQuantity deben rechazar antes del service.
    void create_retorna400_cuandoFaltanCamposObligatorios() throws Exception {
        UUID productId = UUID.randomUUID();
        String invalidJson = """
                {"requiredQuantity":0}
                """;

        mockMvc.perform(post("/api/products/" + productId + "/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_retorna204() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID ingredientId = UUID.randomUUID();

        mockMvc.perform(delete("/api/products/" + productId + "/recipes/" + ingredientId))
                .andExpect(status().isNoContent());
    }
}
