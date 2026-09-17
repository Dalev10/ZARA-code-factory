package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo de la activación de MFA: requiere un JWT real (obtenido
 * vía login), a diferencia de login/refresh/logout que son públicos.
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class MfaControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

    private String crearUsuarioYLoguear(String email) throws Exception {
        Usuario usuario = Usuario.reconstruir(
                UUID.randomUUID(), Email.de(email), "Usuario MFA",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")),
                EstadoUsuario.ACTIVO, 0, null, false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(usuario);

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "contraseñaSegura123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = new ObjectMapper().readTree(login.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    @Test
    void activarSinAutenticacionSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/mfa/activar"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void activarDevuelveSecretoYQrValidos() throws Exception {
        String accessToken = crearUsuarioYLoguear("mfa-activar@ejemplo.com");

        mockMvc.perform(post("/api/v1/mfa/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secreto").isNotEmpty())
                .andExpect(jsonPath("$.qrCodeDataUri").value(
                        org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }

    @Test
    void flujoCompletoActivarYConfirmarHabilitaMfaYDevuelveCodigosDeRespaldo() throws Exception {
        String accessToken = crearUsuarioYLoguear("mfa-completo@ejemplo.com");

        MvcResult activar = mockMvc.perform(post("/api/v1/mfa/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        String secreto = new ObjectMapper().readTree(activar.getResponse().getContentAsString())
                .get("secreto").asText();

        String codigoValido = new DefaultCodeGenerator().generate(secreto,
                new SystemTimeProvider().getTime() / 30);

        mockMvc.perform(post("/api/v1/mfa/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "%s"}
                                """.formatted(codigoValido)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosRespaldo", org.hamcrest.Matchers.hasSize(8)));
    }

    @Test
    void confirmarConCodigoIncorrectoSeRechazaCon400() throws Exception {
        String accessToken = crearUsuarioYLoguear("mfa-codigo-malo@ejemplo.com");

        mockMvc.perform(post("/api/v1/mfa/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/mfa/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El código ingresado no es válido"));
    }

    @Test
    void confirmarSinHaberActivadoAntesSeRechazaCon400() throws Exception {
        String accessToken = crearUsuarioYLoguear("mfa-sin-activar@ejemplo.com");

        mockMvc.perform(post("/api/v1/mfa/confirmar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "123456"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Primero debés iniciar la activación de MFA"));
    }
}
