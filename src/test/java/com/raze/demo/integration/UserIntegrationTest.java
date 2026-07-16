package com.raze.demo.integration;

import com.raze.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — mismo patrón que {@link BranchIntegrationTest}.
 *
 * Cubre el flujo crítico de creación de usuarios: que la contraseña se cifre de verdad con
 * el {@code PasswordEncoder} real de Spring Security (BCrypt) antes de llegar a Postgres.
 * Esto es justo lo que un test de service con PasswordEncoder mockeado NO puede probar,
 * porque ahí el "cifrado" es solo lo que nosotros le dijimos al mock que devolviera.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void flujoCompleto_crearUsuario_cifraContrasenaAntesDeGuardar() throws Exception {
        String email = "usuario-" + UUID.randomUUID() + "@example.com";
        String json = """
                {"email":"%s","password":"SuperSecreta123","firstName":"Ana","lastName":"Lopez","role":"ADMIN"}
                """.formatted(email);

        // POST real: pasa por Bean Validation, el controller real, el service real
        // (que llama al PasswordEncoder real) y un INSERT real contra Postgres.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                // UserResponse nunca expone la contraseña ni su hash.
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Se consulta directo el repositorio real (sin pasar por el controller) para
        // inspeccionar la fila que quedó en Postgres.
        String storedHash = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow()
                .getPasswordHash();

        assertThat(storedHash).isNotEqualTo("SuperSecreta123");
        // BCrypt (el PasswordEncoder configurado en PasswordEncoderConfig) siempre produce
        // hashes que empiezan con "$2".
        assertThat(storedHash).startsWith("$2");
    }

    @Test
    void create_retorna409_cuandoCorreoYaRegistrado() throws Exception {
        String email = "duplicado-" + UUID.randomUUID() + "@example.com";
        String json = """
                {"email":"%s","password":"SuperSecreta123","firstName":"Ana","lastName":"Lopez","role":"ADMIN"}
                """.formatted(email);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Mismo correo otra vez: el service real debe detectar el duplicado contra
        // Postgres (no solo contra un mock) y devolver 409.
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }
}
