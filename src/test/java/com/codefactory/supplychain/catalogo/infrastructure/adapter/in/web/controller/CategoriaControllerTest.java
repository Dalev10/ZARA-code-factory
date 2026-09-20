package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Usuario;
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
 * Prueba de extremo a extremo de Categoria (FEAT-05). Sin guard de scope
 * (no está en alcance de este refactor); solo requiere autenticación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CategoriaControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

    private String loguearUsuario(String email) throws Exception {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario de Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(usuario);
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "contraseñaSegura123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return new ObjectMapper().readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void crearUnaCategoriaDevuelve201() throws Exception {
        String token = loguearUsuario("usuario-crea-categoria@ejemplo.com");

        mockMvc.perform(post("/api/v1/categorias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Calzado"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Calzado"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void crearSinAutenticacionSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Sin Auth"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void crearConNombreVacioDevuelve400() throws Exception {
        String token = loguearUsuario("usuario-categoria-invalida@ejemplo.com");

        mockMvc.perform(post("/api/v1/categorias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void obtenerUnaCategoriaInexistenteDevuelve404() throws Exception {
        String token = loguearUsuario("usuario-categoria-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/categorias/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void modificarYLuegoEliminarUnaCategoria() throws Exception {
        String token = loguearUsuario("usuario-modifica-categoria@ejemplo.com");
        MvcResult creada = mockMvc.perform(post("/api/v1/categorias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Categoria Original"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/categorias/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Categoria Renombrada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Categoria Renombrada"));

        mockMvc.perform(delete("/api/v1/categorias/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categorias/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
