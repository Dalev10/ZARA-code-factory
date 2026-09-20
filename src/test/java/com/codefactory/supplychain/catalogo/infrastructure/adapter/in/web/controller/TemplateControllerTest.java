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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo de Template (FEAT-05), incluyendo la relación
 * obligatoria con Categoria (categoriaId).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TemplateControllerTest {

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

    private String crearCategoria(String token, String nombre) throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/v1/categorias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "%s"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        return new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void crearUnTemplateConCategoriaValidaDevuelve201() throws Exception {
        String token = loguearUsuario("usuario-crea-template@ejemplo.com");
        String categoriaId = crearCategoria(token, "Calzado Template");

        mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Zapatilla X", "temporada": "Verano", "proveedor": "Acme",
                                 "precioBase": 100000, "categoriaId": "%s"}
                                """.formatted(categoriaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Zapatilla X"));
    }

    @Test
    void crearConCategoriaInexistenteDevuelve404() throws Exception {
        String token = loguearUsuario("usuario-template-categoria-404@ejemplo.com");

        mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Sin Categoria", "categoriaId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearConPrecioNegativoDevuelve400() throws Exception {
        String token = loguearUsuario("usuario-template-precio-negativo@ejemplo.com");
        String categoriaId = crearCategoria(token, "Categoria Precio Negativo");

        mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Precio Invalido", "precioBase": -1, "categoriaId": "%s"}
                                """.formatted(categoriaId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificarUnTemplateExistente() throws Exception {
        String token = loguearUsuario("usuario-modifica-template@ejemplo.com");
        String categoriaId = crearCategoria(token, "Categoria Para Modificar Template");
        MvcResult creado = mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Template Original", "categoriaId": "%s"}
                                """.formatted(categoriaId)))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/templates/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Template Renombrado", "categoriaId": "%s"}
                                """.formatted(categoriaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Template Renombrado"));
    }

    @Test
    void obtenerUnTemplateInexistenteDevuelve404() throws Exception {
        String token = loguearUsuario("usuario-template-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/templates/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
