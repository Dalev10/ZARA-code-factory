package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo de CentroDistribucion, incluyendo el guard de
 * autorización por scope "cd:administrar" (HU-20) y el ciclo de vida
 * corregido en HU-22 (DELETE elimina el Nodo asociado, y los status code de
 * duplicado/no-encontrado ya son 409/404 en vez de 400).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CentroDistribucionControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

    @Autowired
    private RolRepositoryPort rolRepositoryPort;

    @Autowired
    private UsuarioRolJpaRepository usuarioRolJpaRepository;

    private String obtenerAccessToken(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = new ObjectMapper().readTree(login.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private Usuario crearUsuario(String email) {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario de Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        return usuarioRepositoryPort.guardar(usuario);
    }

    private String loguearComoAdmin(String email) throws Exception {
        Usuario usuario = crearUsuario(email);
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(usuario.getId(), rolAdminId)));
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    private String loguearComoUsuarioComun(String email) throws Exception {
        crearUsuario(email);
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    @Test
    void crearComoUsuarioSinScopeSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("usuario-comun-cd@ejemplo.com");

        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Rechazado", "ubicacion": "Test"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearUnCdDevuelve201YProvisionaSuNodo() throws Exception {
        String token = loguearComoAdmin("usuario-crea-cd@ejemplo.com");

        MvcResult creado = mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Bogotá", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("CD Bogotá"))
                .andReturn();
        String id = new ObjectMapper().readTree(creado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/centros-distribucion/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodo.tipo").value("CD"))
                .andExpect(jsonPath("$.nodo.cdId").value(id));
    }

    @Test
    void crearConNombreDuplicadoDevuelve409() throws Exception {
        String token = loguearComoAdmin("usuario-duplica-cd@ejemplo.com");
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Duplicado Http", "ubicacion": "Cali"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Duplicado Http", "ubicacion": "Otra"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarUnCdInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("usuario-cd-no-encontrado@ejemplo.com");

        mockMvc.perform(get("/api/v1/centros-distribucion/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listarConFiltroPorNombreParcial() throws Exception {
        String token = loguearComoAdmin("usuario-filtra-cd@ejemplo.com");
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Norte Filtrable", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/centros-distribucion").param("nombre", "norte filtrable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("CD Norte Filtrable"));
    }

    @Test
    void listarSinFiltrosFuncionaComoGetAll() throws Exception {
        String token = loguearComoAdmin("usuario-lista-todos-cd@ejemplo.com");
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Sin Filtros Http", "ubicacion": "Medellín"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    void listarConSortInvalidoDevuelve400EnVezDe500() throws Exception {
        String token = loguearComoAdmin("usuario-sort-invalido-cd@ejemplo.com");

        mockMvc.perform(get("/api/v1/centros-distribucion").param("sort", "[]")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarUnCdConNodoAsociadoDevuelve204YEliminaElNodo() throws Exception {
        String token = loguearComoAdmin("usuario-elimina-cd-huerfano@ejemplo.com");
        MvcResult creado = mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD A Eliminar Http", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creado.getResponse().getContentAsString()).get("id").asText();

        // HU-22: el CD siempre tiene su Nodo asociado (aprovisionado al crearlo);
        // eliminarCentroDistribucion ahora lo elimina primero, así que el DELETE
        // ya no falla por la FK nodo.cd_id.
        mockMvc.perform(delete("/api/v1/centros-distribucion/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/centros-distribucion/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
