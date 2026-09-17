package com.codefactory.supplychain.identity.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.domain.model.Usuario;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del guard genérico de autorización por scope
 * (HU-11): demuestra que el mismo access token, sin renovarse, pierde o gana
 * acceso apenas cambia la asignación de scopes en la base — porque
 * JwtAuthenticationFilter resuelve los scopes del usuario en cada request en
 * vez de embeberlos en el JWT al emitirlo.
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ScopeAuthorizationGuardTest {

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
    private UsuarioRolRepositoryPort usuarioRolRepositoryPort;

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

    @Test
    void otorgarYLuegoRevocarUnScopeAplicaDeInmediatoSinReloguear() throws Exception {
        String email = "guard-en-vivo@ejemplo.com";
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Guard En Vivo",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(usuario);
        Rol rol = rolRepositoryPort.guardar(Rol.crear("ROL_GUARD_EN_VIVO", null));
        usuarioRolRepositoryPort.asignar(usuario.getId(), rol.getId());

        String token = obtenerAccessToken(email, "contraseñaSegura123");

        // Sin el scope todavía: el endpoint (que exige "usuarios:administrar") rechaza.
        mockMvc.perform(get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        // Se le otorga "usuarios:administrar" a su rol -> el MISMO token ahora entra,
        // sin volver a loguearse ni refrescar.
        Scope scopeAdministrar = scopeRepositoryPort.buscarPorCodigo("usuarios:administrar").orElseThrow();
        rolScopeRepositoryPort.asignar(rol.getId(), scopeAdministrar.getId());

        mockMvc.perform(get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        // Se le revoca el scope -> el MISMO token pierde el acceso de inmediato.
        rolScopeRepositoryPort.quitar(rol.getId(), scopeAdministrar.getId());

        mockMvc.perform(get("/api/v1/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
