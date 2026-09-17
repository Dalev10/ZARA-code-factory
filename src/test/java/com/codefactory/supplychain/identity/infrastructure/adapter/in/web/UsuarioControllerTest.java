package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordComprometidaPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del registro de usuario: HTTP -> caso de uso -> Postgres real
 * (Testcontainers) -> validación de esquema vía Flyway. El chequeo de HaveIBeenPwned se
 * reemplaza por un stub determinista para no depender de la red real ni de un tercero
 * en los tests.
 *
 * El cuerpo de las peticiones se arma a mano (sin ObjectMapper inyectado) porque Spring
 * Boot 4.1.1 registra por defecto un ObjectMapper de Jackson 3.x (tools.jackson.databind),
 * no el com.fasterxml.jackson.databind clásico — evita acoplar el test a esa decisión interna.
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test),
 * necesario porque el contexto completo también instancia JjwtAccessTokenAdapter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UsuarioControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class FakeHibpConfig {

        @Bean
        @Primary
        PasswordComprometidaPort passwordComprometidaPortFake() {
            return password -> false;
        }
    }

    @Test
    void registraUnUsuarioNuevoYDevuelve201ConSusDatos() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("nuevo@ejemplo.com", "Nuevo Usuario", "contraseñaSegura123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("nuevo@ejemplo.com"))
                .andExpect(jsonPath("$.nombreCompleto").value("Nuevo Usuario"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void rechazaPasswordMasCortaQueLaPoliticaCon400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("otro@ejemplo.com", "Otro Usuario", "corta123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaEmailConFormatoInvalidoCon400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrarUsuarioJson("no-es-un-email", "Alguien", "contraseñaSegura123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaEmailDuplicadoCon409() throws Exception {
        String cuerpo = registrarUsuarioJson("duplicado@ejemplo.com", "Persona Uno", "contraseñaSegura123");

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict());
    }

    private static String registrarUsuarioJson(String email, String nombreCompleto, String password) {
        return """
                {
                  "email": "%s",
                  "nombreCompleto": "%s",
                  "password": "%s"
                }
                """.formatted(email, nombreCompleto, password);
    }
}
