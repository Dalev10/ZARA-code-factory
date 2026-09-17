package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Rol;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del CRUD de Roles (HU-09), incluyendo el guard de
 * autorización por scope de HU-11 (requiere "roles:administrar").
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RolControllerTest {

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

    private String loguearComoAdmin(String email) throws Exception {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Admin de Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(usuario);
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(usuario.getId(), rolAdminId)));
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    private String loguearComoUsuarioComun(String email) throws Exception {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario Común",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(usuario);
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    @Test
    void crearComoAdminDevuelve201() throws Exception {
        String token = loguearComoAdmin("admin-crea-rol@ejemplo.com");

        mockMvc.perform(post("/api/v1/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "ROL_NUEVO", "descripcion": "desc"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("ROL_NUEVO"));
    }

    @Test
    void crearComoUsuarioComunSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("no-admin-crea-rol@ejemplo.com");

        mockMvc.perform(post("/api/v1/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "ROL_RECHAZADO", "descripcion": "desc"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tenés permisos para realizar esta acción"));
    }

    @Test
    void crearSinAutenticacionSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "ROL_SIN_AUTH", "descripcion": "desc"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void crearConNombreDuplicadoDevuelve409() throws Exception {
        String token = loguearComoAdmin("admin-duplica-rol@ejemplo.com");
        rolRepositoryPort.guardar(Rol.crear("ROL_DUPLICADO_HTTP", null));

        mockMvc.perform(post("/api/v1/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "ROL_DUPLICADO_HTTP", "descripcion": "desc"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerUnRolInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("admin-obtiene-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/roles/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarUnRolExistenteDevuelveLosNuevosValores() throws Exception {
        String token = loguearComoAdmin("admin-actualiza-rol@ejemplo.com");
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_A_ACTUALIZAR_HTTP", "desc vieja"));

        mockMvc.perform(put("/api/v1/roles/" + rol.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "ROL_ACTUALIZADO_HTTP", "descripcion": "desc nueva"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("ROL_ACTUALIZADO_HTTP"))
                .andExpect(jsonPath("$.descripcion").value("desc nueva"));
    }

    @Test
    void eliminarUnRolSinUsuariosAsignadosDevuelve204() throws Exception {
        String token = loguearComoAdmin("admin-elimina-rol@ejemplo.com");
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_A_ELIMINAR_HTTP", null));

        mockMvc.perform(delete("/api/v1/roles/" + rol.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/roles/" + rol.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnRolConUsuariosAsignadosSeRechazaCon400() throws Exception {
        String token = loguearComoAdmin("admin-elimina-rol-en-uso@ejemplo.com");
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/roles/" + rolAdminId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El rol tiene usuarios asignados; quitá esas asignaciones antes de eliminarlo"));
    }

    @Test
    void asignarYQuitarUnScopeDeUnRol() throws Exception {
        String token = loguearComoAdmin("admin-asigna-scope@ejemplo.com");
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_PARA_SCOPES_HTTP", null));
        MvcResult scopeCreado = mockMvc.perform(post("/api/v1/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "scope:http-test", "descripcion": "desc", "sensible": false}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String scopeId = new ObjectMapper().readTree(scopeCreado.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/v1/roles/" + rol.getId() + "/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scopeId": "%s"}
                                """.formatted(scopeId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/roles/" + rol.getId() + "/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("scope:http-test"));

        mockMvc.perform(delete("/api/v1/roles/" + rol.getId() + "/scopes/" + scopeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/roles/" + rol.getId() + "/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
