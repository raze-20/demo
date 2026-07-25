package com.raze.demo.controller;

import com.raze.demo.dto.EmployeeRequest;
import com.raze.demo.dto.EmployeeResponse;
import com.raze.demo.enums.UserRole;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.security.JwtService;
import com.raze.demo.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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
 * {@code /{userId}} en vez de {@code /{id}} porque Employee comparte clave primaria con User.
 */
@WebMvcTest(EmployeeController.class)
@WithMockUser(roles = "ADMIN")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getById_retorna200_conEmployee() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        EmployeeResponse response = new EmployeeResponse(
                userId, "empleado@example.com", "Ana", "Gomez",
                branchId, "Sucursal Centro", "Barista", "BARISTA", LocalDate.of(2026, 1, 15));
        when(employeeService.findById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/employees/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("Barista"))
                .andExpect(jsonPath("$.branchName").value("Sucursal Centro"));
    }

    @Test
    void getById_retorna404_cuandoNoExiste() throws Exception {
        UUID userId = UUID.randomUUID();
        when(employeeService.findById(userId)).thenThrow(new ResourceNotFoundException("Employee not found: " + userId));

        mockMvc.perform(get("/api/employees/" + userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        EmployeeRequest request = new EmployeeRequest(
                "empleado@example.com", "password123", "Ana", "Gomez",
                UserRole.BARISTA, branchId, "Barista", LocalDate.of(2026, 1, 15));
        EmployeeResponse response = new EmployeeResponse(
                userId, "empleado@example.com", "Ana", "Gomez",
                branchId, "Sucursal Centro", "Barista", "BARISTA", LocalDate.of(2026, 1, 15));
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    // @NotBlank/@NotNull en email/password/branchId/hireDate/type/position deben rechazar
    // la petición antes de tocar el service.
    void create_retorna400_cuandoFaltanCamposObligatorios() throws Exception {
        String invalidJson = """
                {"position":"Barista"}
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_retorna204() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/employees/" + userId))
                .andExpect(status().isNoContent());
    }
}
