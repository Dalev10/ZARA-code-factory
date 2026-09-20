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
 * autorización por scope "cd:administrar" (HU-20). El bug conocido de DELETE
 * (huérfana el Nodo asociado) se deja intacto a propósito — pertenece a
 * HU-22 — y se verifica aquí que sigue comportándose igual.
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
    void crearConNombreDuplicadoDevuelveBadRequestPorLaDeudaDeStatusCodeDeferida() throws Exception {
        String token = loguearComoAdmin("usuario-duplica-cd@ejemplo.com");
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Duplicado Http", "ubicacion": "Cali"}
                                """))
                .andExpect(status().isCreated());

        // Comportamiento heredado y deliberadamente sin corregir en este refactor:
        // CentroDistribucionDuplicadoException también extiende ReglaDeNegocioException
        // (400), no RecursoDuplicadoException (409). Ver auditoría / deuda técnica de FEAT-04.
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Duplicado Http", "ubicacion": "Otra"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarUnCdInexistenteDevuelveBadRequestPorLaDeudaDeStatusCodeDeferida() throws Exception {
        String token = loguearComoAdmin("usuario-cd-no-encontrado@ejemplo.com");

        // Comportamiento heredado y deliberadamente sin corregir en este refactor:
        // CentroDistribucionNoEncontradaException extiende ReglaDeNegocioException (400),
        // no RecursoNoEncontradoException (404). Ver auditoría / deuda técnica de FEAT-04.
        mockMvc.perform(get("/api/v1/centros-distribucion/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$[0].nombre").value("CD Norte Filtrable"));
    }

    @Test
    void eliminarUnCdConNodoAsociadoDevuelve409EnVezDeUnErrorSinManejar() throws Exception {
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

        // Deuda técnica conocida, deliberadamente NO corregida todavía (pertenece a
        // HU-22): eliminar un CD no elimina su Nodo asociado primero, así que la
        // violación de llave foránea sigue ocurriendo. Lo que sí cambió con HU-19 es que
        // GlobalExceptionHandler ahora traduce esa DataIntegrityViolationException a un
        // 409 uniforme en vez de dejarla propagar como una excepción sin manejar (500).
        mockMvc.perform(delete("/api/v1/centros-distribucion/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict());
    }
}
