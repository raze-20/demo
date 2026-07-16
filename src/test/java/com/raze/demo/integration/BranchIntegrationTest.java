package com.raze.demo.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST DE INTEGRACIÓN COMPLETO — el más lento y el más "real" de los tres tipos.
 *
 * A diferencia del test de service (sin Spring) y del test de controller (Spring parcial,
 * sin base de datos), aquí se levanta la aplicación ENTERA: todos los @Service, @Repository,
 * @Controller reales, conectados a una base de datos Postgres real (corriendo en un
 * contenedor Docker efímero vía Testcontainers) con las migraciones de Flyway aplicadas.
 *
 * Esto prueba el flujo end-to-end real: HTTP -> Controller -> Service -> JPA/Hibernate ->
 * SQL -> Postgres -> de vuelta. Es lo único que hubiera detectado el bug real que encontramos
 * (branches nuevas quedaban con active=false por defecto y no aparecían en findAll()),
 * porque un mock nunca replicaría ese comportamiento del campo por defecto de la entidad.
 *
 * Requisito para poder correr esta clase: tener Docker disponible localmente/CI, ya que
 * Testcontainers necesita un daemon de Docker para poder levantar el contenedor de Postgres.
 */
@Testcontainers
// Extensión de JUnit 5 (de la librería Testcontainers, no de Spring) que gestiona el
// ciclo de vida de los contenedores marcados con @Container: los arranca antes de los
// tests de la clase y los apaga al terminar.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Carga el ApplicationContext COMPLETO de Spring Boot, tal cual arrancaría en producción
// (todos los beans reales, sin mocks), y además levanta un servidor embebido (Tomcat) real
// en un puerto libre aleatorio. Es la anotación más "cara" de las tres usadas en el proyecto.
@AutoConfigureMockMvc
// Aunque ya hay un servidor real corriendo (por RANDOM_PORT), esta anotación configura
// un MockMvc conectado al contexto real, para poder seguir escribiendo los tests con la
// misma API mockMvc.perform(...) que en el test de controller, sin tener que hacer
// llamadas HTTP de verdad por sockets (aunque también se podría, con TestRestTemplate).
class BranchIntegrationTest {

    @Container
    // Campo estático administrado por @Testcontainers: aquí se declara el contenedor
    // real de Postgres 16 que se va a descargar/arrancar con Docker antes de los tests.
    @ServiceConnection
    // Característica de Spring Boot: conecta automáticamente el DataSource de la app
    // (URL, usuario, password) a este contenedor, sin tener que escribir manualmente
    // un @DynamicPropertySource. Sustituye por completo la configuración de application.yml.
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    // A diferencia del test de controller, este MockMvc no está atado a un slice: viaja
    // por TODA la cadena real (filtros, controller, service, repositorio JPA, Hibernate,
    // SQL contra el Postgres del contenedor).
    private MockMvc mockMvc;

    @Test
    // Para correr, este método necesita: el contenedor de Postgres ya levantado y con
    // las migraciones de Flyway aplicadas (lo hace Spring Boot al arrancar el contexto),
    // y el contexto completo de Spring inyectado. No hay ningún mock en todo el archivo:
    // cada capa (controller, service, repositorio, base de datos) es la real.
    void flujoCompleto_crearYConsultarSucursal() throws Exception {
        String json = """
                {"name":"Sucursal Sur","address":"Blvd 99","city":"Guadalajara","state":"JAL"}
                """;

        // POST real: pasa por Bean Validation, el controller real, el service real
        // (que efectivamente hace INSERT en Postgres) y devuelve la fila ya creada.
        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        // GET real: consulta de nuevo Postgres (SELECT ... WHERE active = true) y
        // confirma que la sucursal recién creada sí quedó visible ahí. Este segundo
        // paso es justo el que reveló que Branch.active quedaba en false por defecto.
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sucursal Sur"));
    }
}
