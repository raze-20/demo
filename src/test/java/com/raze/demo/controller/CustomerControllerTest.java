package com.raze.demo.controller;

import com.raze.demo.dto.CustomerRequest;
import com.raze.demo.dto.CustomerResponse;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link BranchControllerTest}. La ruta usa
 * {@code /{userId}} en vez de {@code /{id}} porque Customer comparte clave primaria con User.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void getById_retorna200_conCustomer() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomerResponse response = new CustomerResponse(userId, "cliente@example.com", "Juan", "Perez", 10, LocalDate.of(2000, 1, 1));
        when(customerService.findById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/customers/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyaltyPoints").value(10));
    }

    @Test
    void getById_retorna404_cuandoNoExiste() throws Exception {
        UUID userId = UUID.randomUUID();
        when(customerService.findById(userId)).thenThrow(new ResourceNotFoundException("Customer not found: " + userId));

        mockMvc.perform(get("/api/customers/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomerRequest request = new CustomerRequest("cliente@example.com", "password123", "Juan", "Perez", 0, LocalDate.of(1998, 5, 12));
        CustomerResponse response = new CustomerResponse(userId, "cliente@example.com", "Juan", "Perez", 0, LocalDate.of(1998, 5, 12));
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    // @NotBlank en email/password/firstName/lastName debe rechazar la petición antes de
    // tocar el service.
    void create_retorna400_cuandoFaltanCamposObligatorios() throws Exception {
        String invalidJson = """
                {"loyaltyPoints":0,"birthDate":"1998-05-12"}
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_retorna204() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/" + userId))
                .andExpect(status().isNoContent());
    }
}
