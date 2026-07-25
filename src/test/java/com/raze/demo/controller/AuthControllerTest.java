package com.raze.demo.controller;

import com.raze.demo.dto.AuthRequest;
import com.raze.demo.enums.UserRole;
import com.raze.demo.security.JwtService;
import com.raze.demo.security.UserPrincipal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE "SLICE" (capa web) — mismo patrón que {@link ProductControllerTest}. Mockea
 * {@link AuthenticationManager} y {@link JwtService} para no depender de una BD real; el
 * login real de punta a punta (contra Postgres) se cubre en el test de integración.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void login_retorna200_conToken_cuandoCredencialesValidas() throws Exception {
        AuthRequest request = new AuthRequest("ana@example.com", "SuperSecreta123");
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), "ana@example.com", "hash", UserRole.ADMIN, true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal.getId(), principal.getEmail(), "ADMIN")).thenReturn("fake.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_retorna401_cuandoCredencialesInvalidas() throws Exception {
        AuthRequest request = new AuthRequest("ana@example.com", "incorrecta");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    // Bean Validation (@Email/@NotBlank) debe rechazar la petición antes de tocar el
    // AuthenticationManager.
    void login_retorna400_cuandoCorreoMalFormado() throws Exception {
        String invalidJson = """
                {"email":"no-es-un-correo","password":"SuperSecreta123"}
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
