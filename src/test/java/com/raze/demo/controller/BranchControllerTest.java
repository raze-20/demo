package com.raze.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raze.demo.dto.BranchRequest;
import com.raze.demo.dto.BranchResponse;
import com.raze.demo.service.BranchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BranchController.class)
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BranchService branchService;

    @Test
    void getById_retorna200_conSucursal() throws Exception {
        BranchResponse response = new BranchResponse(1L, "Sucursal Centro", "Av. Principal 123", "Ciudad de Mexico", "CDMX");
        when(branchService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/branches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sucursal Centro"));
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        BranchRequest request = new BranchRequest("Sucursal Norte", "Calle 45", "Monterrey", "NL");
        BranchResponse response = new BranchResponse(2L, "Sucursal Norte", "Calle 45", "Monterrey", "NL");
        when(branchService.create(any(BranchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void create_retorna400_cuandoFaltaNombre() throws Exception {
        String invalidJson = """
                {"address":"Calle 45","city":"Monterrey","state":"NL"}
                """;

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}