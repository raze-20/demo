package com.raze.coffeeshop.controller;

import com.raze.coffeeshop.dto.UserRequest;
import com.raze.coffeeshop.dto.UserResponse;
import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.exception.DuplicateResourceException;
import com.raze.coffeeshop.security.JwtService;
import com.raze.coffeeshop.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link BranchControllerTest}.
 */
@WebMvcTest(UserController.class)
@WithMockUser(roles = "ADMIN")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getById_retorna200_conUsuario() throws Exception {
        UUID id = UUID.randomUUID();
        UserResponse response = new UserResponse(id, "ana@example.com", "Ana", "Lopez", UserRole.ADMIN, true, OffsetDateTime.now());
        when(userService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                // La contraseña (ni en texto plano ni el hash) nunca debe viajar en la
                // respuesta: UserResponse ni siquiera tiene ese campo.
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID id = UUID.randomUUID();
        UserRequest request = new UserRequest("ana@example.com", "SuperSecreta123", "Ana", "Lopez", UserRole.ADMIN);
        UserResponse response = new UserResponse(id, "ana@example.com", "Ana", "Lopez", UserRole.ADMIN, true, OffsetDateTime.now());
        when(userService.create(any(UserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    // @Email en UserRequest.email debe rechazar un correo mal formado antes de que el
    // controller invoque al service.
    void create_retorna400_cuandoCorreoInvalido() throws Exception {
        String invalidJson = """
                {"email":"no-es-un-correo","password":"SuperSecreta123","firstName":"Ana","lastName":"Lopez","role":"ADMIN"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    // @Size(min = 8) en UserRequest.password debe rechazar contraseñas demasiado cortas.
    void create_retorna400_cuandoContrasenaMuyCorta() throws Exception {
        String invalidJson = """
                {"email":"ana@example.com","password":"123","firstName":"Ana","lastName":"Lopez","role":"ADMIN"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_retorna409_cuandoCorreoDuplicado() throws Exception {
        UserRequest request = new UserRequest("ana@example.com", "SuperSecreta123", "Ana", "Lopez", UserRole.ADMIN);
        when(userService.create(any(UserRequest.class)))
                .thenThrow(new DuplicateResourceException("User already exists: ana@example.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_retorna204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/users/" + id))
                .andExpect(status().isNoContent());
    }
}
