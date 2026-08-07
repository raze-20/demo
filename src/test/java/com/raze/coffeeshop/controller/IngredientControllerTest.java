package com.raze.coffeeshop.controller;

import com.raze.coffeeshop.dto.IngredientRequest;
import com.raze.coffeeshop.dto.IngredientResponse;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.security.JwtService;
import com.raze.coffeeshop.service.IngredientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link BranchControllerTest}.
 */
@WebMvcTest(IngredientController.class)
@WithMockUser(roles = "ADMIN")
class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngredientService ingredientService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getById_retorna200_conIngrediente() throws Exception {
        UUID id = UUID.randomUUID();
        IngredientResponse response = new IngredientResponse(id, "Leche entera", "ml");
        when(ingredientService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/ingredients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Leche entera"));
    }

    @Test
    void getById_retorna404_cuandoNoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        when(ingredientService.findById(id)).thenThrow(new ResourceNotFoundException("Ingredient not found: " + id));

        mockMvc.perform(get("/api/v1/ingredients/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID id = UUID.randomUUID();
        IngredientRequest request = new IngredientRequest("Leche de almendra", "ml");
        IngredientResponse response = new IngredientResponse(id, "Leche de almendra", "ml");
        when(ingredientService.create(any(IngredientRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    // Bean Validation (@NotBlank en name y measureUnit) debe rechazar la petición antes
    // de invocar al service.
    void create_retorna400_cuandoFaltaUnidadDeMedida() throws Exception {
        String invalidJson = """
                {"name":"Leche de almendra"}
                """;

        mockMvc.perform(post("/api/v1/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_retorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/ingredients/" + id))
                .andExpect(status().isNoContent());
    }
}
