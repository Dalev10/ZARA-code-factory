package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioRolJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de extremo a extremo del historial y registro manual de traspasos
 * internos (HU-39, HU-40), incluyendo el guard por scope {@code reposicion:administrar}.
 *
 * Módulo: reposicion — Reposición interna.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("TraspasoInternoController")
class TraspasoInternoControllerTest {

    private static final String RUTA_TIENDA   = "/api/v1/tiendas/{tiendaId}/traspasos-internos";
    private static final String RUTA_DETALLE  = "/api/v1/traspasos-internos/{id}";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final ObjectMapper json = new ObjectMapper();

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepositoryPort usuarioRepositoryPort;
    @Autowired private PasswordHasherPort passwordHasherPort;
    @Autowired private RolRepositoryPort rolRepositoryPort;
    @Autowired private UsuarioRolJpaRepository usuarioRolJpaRepository;

    // ---- helpers de autenticación -------------------------------------------

    private String obtenerAccessToken(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private Usuario crearUsuario(String email) {
        Usuario usuario = Usuario.reconstruir(
                UUID.randomUUID(), Email.de(email), "Operario Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")),
                EstadoUsuario.ACTIVO, 0, null, false, null, null,
                Instant.now(), Instant.now());
        return usuarioRepositoryPort.guardar(usuario);
    }

    private String loguearComoAdmin(String email) throws Exception {
        Usuario usuario = crearUsuario(email);
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        usuarioRolJpaRepository.save(
                new UsuarioRolEntity(new UsuarioRolId(usuario.getId(), rolAdminId)));
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    private String loguearComoUsuarioComun(String email) throws Exception {
        crearUsuario(email);
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    // ---- escenarios: seguridad ----------------------------------------------

    @Test
    @DisplayName("POST sin token devuelve 401")
    void registrarManualSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post(RUTA_TIENDA, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"varianteId": "%s", "cantidad": 1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST sin scope reposicion:administrar devuelve 403")
    void registrarManualSinScopeDevuelve403() throws Exception {
        String token = loguearComoUsuarioComun("traspaso-403@ejemplo.com");

        mockMvc.perform(post(RUTA_TIENDA, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"varianteId": "%s", "cantidad": 1}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET historial sin token devuelve 401")
    void listarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(RUTA_TIENDA, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET detalle sin token devuelve 401")
    void detalleSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(RUTA_DETALLE, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ---- escenarios: validación de entrada ----------------------------------

    @Test
    @DisplayName("POST con varianteId nulo devuelve 400")
    void registrarManualSinVarianteDevuelve400() throws Exception {
        String token = loguearComoAdmin("traspaso-sin-variante@ejemplo.com");

        mockMvc.perform(post(RUTA_TIENDA, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidad": 5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST con cantidad 0 devuelve 400")
    void registrarManualConCantidadCeroDevuelve400() throws Exception {
        String token = loguearComoAdmin("traspaso-cantidad-cero@ejemplo.com");

        mockMvc.perform(post(RUTA_TIENDA, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"varianteId": "%s", "cantidad": 0}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    // ---- escenarios: consultas con admin ------------------------------------

    @Test
    @DisplayName("GET historial de tienda sin traspasos devuelve lista vacía con 200")
    void listarPorTiendaSinTraspasoDevuelve200Vacio() throws Exception {
        String token = loguearComoAdmin("traspaso-lista-vacia@ejemplo.com");

        mockMvc.perform(get(RUTA_TIENDA, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET detalle con id inexistente devuelve 404")
    void obtenerConIdInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("traspaso-404@ejemplo.com");

        mockMvc.perform(get(RUTA_DETALLE, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}