package com.raze.coffeeshop.controller;

import com.raze.coffeeshop.dto.BranchRequest;
import com.raze.coffeeshop.dto.BranchResponse;
import com.raze.coffeeshop.security.JwtService;
import com.raze.coffeeshop.service.BranchService;
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
 * TEST DE "SLICE" (capa web) — a diferencia de los tests de service, aquí SÍ se levanta
 * Spring, pero solo un pedazo (slice) del contexto: los componentes MVC (controllers,
 * @ExceptionHandler, conversores JSON, validación @Valid, etc.). NO se cargan los
 * @Service ni @Repository reales, ni hay conexión a base de datos.
 *
 * Es más lento que un test de unidad (hay que arrancar un ApplicationContext), pero mucho
 * más rápido que un test de integración completo, y prueba cosas que el test de service NO
 * puede probar: el mapeo de rutas HTTP, la (de)serialización JSON real, los códigos de
 * estado HTTP y las validaciones de Bean Validation (@NotBlank, @Size, etc. en el DTO).
 */
@WebMvcTest(BranchController.class)
// Le dice a Spring Boot Test: "arranca un contexto mínimo, solo con lo necesario para
// probar BranchController" (MVC + serialización + validación). No registra
// BranchServiceImpl, BranchRepository, ni ningún bean de acceso a datos.
// @WithMockUser inyecta un principal autenticado (rol ADMIN, satisface cualquier regla de
// autorización de la app) sin pasar por login/JWT real.
@WithMockUser(roles = "ADMIN")
class BranchControllerTest {

    @Autowired
    // MockMvc lo provee el propio slice de Spring: permite "simular" peticiones HTTP
    // (GET/POST/PUT/DELETE) directamente contra el DispatcherServlet, en memoria,
    // sin abrir un puerto real de red ni levantar un servidor Tomcat de verdad.
    private MockMvc mockMvc;

    @Autowired
    // ObjectMapper real, tomado del contexto de Spring (el mismo que usaría la app en
    // producción para serializar/deserializar JSON). Se usa aquí para convertir el DTO
    // de request a un String JSON antes de enviarlo en el cuerpo de la petición simulada.
    private ObjectMapper objectMapper;

    @MockitoBean
    // Como @WebMvcTest NO instancia el BranchService real, pero BranchController lo
    // necesita en su constructor, @MockitoBean registra un mock de BranchService DENTRO
    // del contexto de Spring (a diferencia de @Mock, que es un mock "suelto" fuera de
    // cualquier contenedor). Así el controller se puede construir igual que en producción.
    private BranchService branchService;

    @MockitoBean
    // Spring Security se auto-configura para este slice porque spring-boot-starter-security
    // está en el classpath; SecurityConfig necesita poder construir JwtAuthenticationFilter,
    // que depende de JwtService. Este mock nunca se invoca en estos tests (@WithMockUser
    // salta directo al SecurityContext), solo hace falta para que el contexto arranque.
    private JwtService jwtService;

    @Test
    // Para correr necesita: (1) el contexto de Spring ya arrancado por @WebMvcTest,
    // (2) el mock de BranchService devolviendo datos falsos, y (3) MockMvc simulando
    // la petición HTTP real (con su propia ruta, headers, etc.).
    void getById_retorna200_conSucursal() throws Exception {
        UUID id = UUID.randomUUID();
        BranchResponse response = new BranchResponse(id, "Sucursal Centro", "Av. Principal 123", "Ciudad de Mexico", "CDMX", OffsetDateTime.now());
        when(branchService.findById(id)).thenReturn(response);

        // mockMvc.perform(get(...)) ejecuta el ciclo real de Spring MVC: resuelve la ruta,
        // llama al método del controller, serializa la respuesta a JSON y arma un
        // HttpServletResponse simulado que luego se puede inspeccionar con andExpect(...).
        mockMvc.perform(get("/api/v1/branches/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sucursal Centro"));
    }

    @Test
    void create_retorna201_conCuerpoValido() throws Exception {
        UUID id = UUID.randomUUID();
        BranchRequest request = new BranchRequest("Sucursal Norte", "Calle 45", "Monterrey", "NL");
        BranchResponse response = new BranchResponse(id, "Sucursal Norte", "Calle 45", "Monterrey", "NL", OffsetDateTime.now());
        when(branchService.create(any(BranchRequest.class))).thenReturn(response);

        // Se envía JSON de verdad (serializado con el ObjectMapper real) para forzar que
        // Spring MVC ejercite su deserializador y el @Valid del controller, cosa que un
        // test de service (que llama directo al método Java) nunca pasa por ahí.
        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    // Este test NO configura ningún mock (when(...)) porque nunca debería llegar a
    // invocar al service: la petición debe ser rechazada antes, por Bean Validation
    // (falta "name", que es @NotBlank en BranchRequest), devolviendo 400 directamente.
    void create_retorna400_cuandoFaltaNombre() throws Exception {
        String invalidJson = """
                {"address":"Calle 45","city":"Monterrey","state":"NL"}
                """;

        mockMvc.perform(post("/api/v1/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
