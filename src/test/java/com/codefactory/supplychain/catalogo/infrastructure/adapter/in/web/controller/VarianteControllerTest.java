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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo de Variante (FEAT-05), incluyendo la
 * dependencia obligatoria con Template y la unicidad de SKU.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class VarianteControllerTest {

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

    private String crearTemplate(String token, String nombre, String categoriaId) throws Exception {
        MvcResult creado = mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "%s", "categoriaId": "%s"}
                                """.formatted(nombre, categoriaId)))
                .andExpect(status().isCreated())
                .andReturn();
        return new ObjectMapper().readTree(creado.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void crearUnaVarianteConTemplateValidoDevuelve201() throws Exception {
        String token = loguearUsuario("usuario-crea-variante@ejemplo.com");
        String categoriaId = crearCategoria(token, "Categoria Variante");
        String templateId = crearTemplate(token, "Template Variante", categoriaId);

        mockMvc.perform(post("/api/v1/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "SKU-HTTP-001", "talla": "M", "color": "Rojo", "templateId": "%s"}
                                """.formatted(templateId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-HTTP-001"));
    }

    @Test
    void crearConTemplateInexistenteDevuelve404() throws Exception {
        String token = loguearUsuario("usuario-variante-template-404@ejemplo.com");

        mockMvc.perform(post("/api/v1/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "SKU-SIN-TEMPLATE", "templateId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearConSkuDuplicadoDevuelve409() throws Exception {
        String token = loguearUsuario("usuario-variante-sku-duplicado@ejemplo.com");
        String categoriaId = crearCategoria(token, "Categoria Sku Duplicado");
        String templateId = crearTemplate(token, "Template Sku Duplicado", categoriaId);

        mockMvc.perform(post("/api/v1/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "SKU-DUP-HTTP", "templateId": "%s"}
                                """.formatted(templateId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "SKU-DUP-HTTP", "templateId": "%s"}
                                """.formatted(templateId)))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerPorSkuDevuelveLaVariante() throws Exception {
        String token = loguearUsuario("usuario-variante-por-sku@ejemplo.com");
        String categoriaId = crearCategoria(token, "Categoria Por Sku");
        String templateId = crearTemplate(token, "Template Por Sku", categoriaId);
        mockMvc.perform(post("/api/v1/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "SKU-CONSULTA", "templateId": "%s"}
                                """.formatted(templateId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/variantes/sku/SKU-CONSULTA")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-CONSULTA"));
    }

    @Test
    void obtenerUnaVarianteInexistenteDevuelve404() throws Exception {
        String token = loguearUsuario("usuario-variante-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/variantes/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
