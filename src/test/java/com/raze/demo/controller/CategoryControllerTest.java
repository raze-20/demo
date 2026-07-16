package com.raze.demo.controller;

import com.raze.demo.dto.CategoryRequest;
import com.raze.demo.dto.CategoryResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link BranchControllerTest}: solo se
 * levanta el slice MVC (controller + validación + serialización JSON), sin service ni
 * repositorio real.
 */
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getById_retorna200_conCategoria() throws Exception {
        CategoryResponse response = new CategoryResponse(1, "Coffee", true);
        when(categoryService.findById(1)).thenReturn(response);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Coffee"));
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        CategoryRequest request = new CategoryRequest("Tea", true);
        CategoryResponse response = new CategoryResponse(2, "Tea", true);
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    // No configura ningún mock porque Bean Validation (@NotBlank en "name") debe rechazar
    // la petición antes de que el controller llegue a invocar al service.
    void create_retorna400_cuandoFaltaNombre() throws Exception {
        String invalidJson = """
                {"active":true}
                """;

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    // El GlobalExceptionHandler debe traducir DuplicateResourceException a un 409, sin que
    // el controller tenga try/catch propio.
    void create_retorna409_cuandoNombreDuplicado() throws Exception {
        CategoryRequest request = new CategoryRequest("Coffee", true);
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new DuplicateResourceException("Category already exists: Coffee"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_retorna204() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
