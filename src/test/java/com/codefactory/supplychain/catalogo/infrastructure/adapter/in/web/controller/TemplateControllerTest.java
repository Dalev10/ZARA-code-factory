package com.codefactory.supplychain.catalogo.infrastructure.adapter.in.web.controller;

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
 * Prueba de extremo a extremo de Template (FEAT-05), incluyendo la relación
 * obligatoria con Categoria (categoriaId) y el guard de autorización por
 * scope "catalogo:administrar" (HU-20).
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
        return new ObjectMapper().readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
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
    void crearComoUsuarioSinScopeSeRechazaCon403() throws Exception {
        String tokenAdmin = loguearComoAdmin("admin-crea-categoria-para-403-template@ejemplo.com");
        String categoriaId = crearCategoria(tokenAdmin, "Categoria Para 403 Template");
        String tokenComun = loguearComoUsuarioComun("usuario-comun-template@ejemplo.com");

        mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Template Rechazado", "categoriaId": "%s"}
                                """.formatted(categoriaId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearUnTemplateConCategoriaValidaDevuelve201() throws Exception {
        String token = loguearComoAdmin("usuario-crea-template@ejemplo.com");
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
        String token = loguearComoAdmin("usuario-template-categoria-404@ejemplo.com");

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
        String token = loguearComoAdmin("usuario-template-precio-negativo@ejemplo.com");
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
        String token = loguearComoAdmin("usuario-modifica-template@ejemplo.com");
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
        String token = loguearComoAdmin("usuario-template-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/templates/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnTemplateConVariantesAsociadasDevuelve409() throws Exception {
        String token = loguearComoAdmin("usuario-template-con-variantes@ejemplo.com");
        String categoriaId = crearCategoria(token, "Categoria Template Con Variante");
        MvcResult creado = mockMvc.perform(post("/api/v1/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Template Con Variante", "categoriaId": "%s"}
                                """.formatted(categoriaId)))
                .andExpect(status().isCreated())
                .andReturn();
        String templateId = new ObjectMapper().readTree(creado.getResponse().getContentAsString())
                .get("id").asText();
        mockMvc.perform(post("/api/v1/variantes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku": "SKU-TEMPLATE-CON-VARIANTE", "templateId": "%s"}
                                """.formatted(templateId)))
                .andExpect(status().isCreated());

        // HU-19: la violación de FK (template_id en variante) ya no llega como un
        // 500 sin manejar — GlobalExceptionHandler la traduce a un 409 uniforme.
        mockMvc.perform(delete("/api/v1/templates/" + templateId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict());
    }
}
