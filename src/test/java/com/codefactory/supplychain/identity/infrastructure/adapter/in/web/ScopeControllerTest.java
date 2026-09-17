package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Scope;
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
 * Prueba de extremo a extremo del CRUD de Scopes (HU-09).
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ScopeControllerTest {

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
    private ScopeRepositoryPort scopeRepositoryPort;

    @Autowired
    private RolScopeRepositoryPort rolScopeRepositoryPort;

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
        String token = loguearComoAdmin("admin-crea-scope@ejemplo.com");

        mockMvc.perform(post("/api/v1/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "scope:nuevo", "descripcion": "desc", "sensible": true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("scope:nuevo"))
                .andExpect(jsonPath("$.sensible").value(true));
    }

    @Test
    void crearComoUsuarioComunSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("no-admin-crea-scope@ejemplo.com");

        mockMvc.perform(post("/api/v1/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "scope:rechazado", "descripcion": "desc", "sensible": false}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearConCodigoDuplicadoDevuelve409() throws Exception {
        String token = loguearComoAdmin("admin-duplica-scope@ejemplo.com");
        scopeRepositoryPort.guardar(Scope.crear("scope:duplicado-http", null, false));

        mockMvc.perform(post("/api/v1/scopes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "scope:duplicado-http", "descripcion": "desc", "sensible": false}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerUnScopeInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("admin-obtiene-scope-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/scopes/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarUnScopeExistenteDevuelveLosNuevosValores() throws Exception {
        String token = loguearComoAdmin("admin-actualiza-scope@ejemplo.com");
        Scope scope = scopeRepositoryPort.guardar(Scope.crear("scope:a-actualizar-http", "vieja", false));

        mockMvc.perform(put("/api/v1/scopes/" + scope.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "scope:actualizado-http", "descripcion": "nueva", "sensible": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("scope:actualizado-http"))
                .andExpect(jsonPath("$.sensible").value(true));
    }

    @Test
    void eliminarUnScopeSinRolesAsignadosDevuelve204() throws Exception {
        String token = loguearComoAdmin("admin-elimina-scope@ejemplo.com");
        Scope scope = scopeRepositoryPort.guardar(Scope.crear("scope:a-eliminar-http", null, false));

        mockMvc.perform(delete("/api/v1/scopes/" + scope.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/scopes/" + scope.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnScopeAsignadoAUnRolSeRechazaCon400() throws Exception {
        String token = loguearComoAdmin("admin-elimina-scope-en-uso@ejemplo.com");
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_PARA_SCOPE_EN_USO", null));
        Scope scope = scopeRepositoryPort.guardar(Scope.crear("scope:en-uso-http", null, false));
        rolScopeRepositoryPort.asignar(rol.getId(), scope.getId());

        mockMvc.perform(delete("/api/v1/scopes/" + scope.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El scope está asignado a uno o más roles; quitá esas asignaciones antes de eliminarlo"));
    }
}
