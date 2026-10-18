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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de extremo a extremo del endpoint de evaluación masiva de reposición
 * (HU-41), incluyendo el guard por scope {@code reposicion:administrar} (D-57).
 *
 * Módulo: reposicion — Reposición interna.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@DisplayName("EvaluacionMasivaController")
class EvaluacionMasivaControllerTest {

    private static final String RUTA_EVALUAR_TODAS  = "/api/v1/reposicion-interna/evaluar";
    private static final String RUTA_EVALUAR_TIENDA = "/api/v1/reposicion-interna/tiendas/{tiendaId}/evaluar";

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
    @DisplayName("POST /evaluar sin token devuelve 401")
    void evaluarTodasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post(RUTA_EVALUAR_TODAS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /tiendas/{id}/evaluar sin token devuelve 401")
    void evaluarTiendaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post(RUTA_EVALUAR_TIENDA, UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /evaluar sin scope reposicion:administrar devuelve 403")
    void evaluarTodasSinScopeDevuelve403() throws Exception {
        String token = loguearComoUsuarioComun("evaluacion-403@ejemplo.com");

        mockMvc.perform(post(RUTA_EVALUAR_TODAS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /tiendas/{id}/evaluar sin scope devuelve 403")
    void evaluarTiendaSinScopeDevuelve403() throws Exception {
        String token = loguearComoUsuarioComun("evaluacion-tienda-403@ejemplo.com");

        mockMvc.perform(post(RUTA_EVALUAR_TIENDA, UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ---- escenarios: respuesta exitosa con bd vacía -------------------------

    @Test
    @DisplayName("POST /evaluar con bd sin tiendas activas devuelve 200 con cero traspasos")
    void evaluarTodasSinTiendasDevuelve200ConCeroTraspasos() throws Exception {
        String token = loguearComoAdmin("evaluacion-vacia@ejemplo.com");

        mockMvc.perform(post(RUTA_EVALUAR_TODAS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traspasoEjecutados").value(0))
                .andExpect(jsonPath("$.traspasos").isArray())
                .andExpect(jsonPath("$.traspasos.length()").value(0));
    }

    @Test
    @DisplayName("POST /tiendas/{id}/evaluar con tienda sin variantes devuelve 200 con cero traspasos")
    void evaluarTiendaSinVariantesDevuelve200ConCeroTraspasos() throws Exception {
        String token = loguearComoAdmin("evaluacion-tienda-vacia@ejemplo.com");
        UUID tiendaId = UUID.randomUUID(); // tienda que no tiene inventario

        mockMvc.perform(post(RUTA_EVALUAR_TIENDA, tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traspasoEjecutados").value(0))
                .andExpect(jsonPath("$.traspasos").isArray());
    }
}