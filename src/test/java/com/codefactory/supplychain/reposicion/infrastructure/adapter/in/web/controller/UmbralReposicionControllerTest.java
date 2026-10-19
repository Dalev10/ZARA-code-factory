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
 * Prueba de extremo a extremo de los umbrales de reposición interna (HU-38), incluyendo
 * el guard por scope {@code reposicion:administrar} (D-57) y la validación de la variante
 * contra catalogo a través del contrato publicado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UmbralReposicionControllerTest {

    private static final String RUTA = "/api/v1/umbrales-reposicion";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final ObjectMapper json = new ObjectMapper();

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

    // ---- helpers -------------------------------------------------------------

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

    private String crearYObtenerId(String ruta, String token, String cuerpo) throws Exception {
        MvcResult creado = mockMvc.perform(post(ruta)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(creado.getResponse().getContentAsString()).get("id").asText();
    }

    private String crearCategoria(String token) throws Exception {
        return crearYObtenerId("/api/v1/categorias", token,
                "{\"nombre\": \"Cat-%s\"}".formatted(UUID.randomUUID()));
    }

    private String crearVariante(String token, String categoriaId) throws Exception {
        String templateId = crearYObtenerId("/api/v1/templates", token, """
                {"nombre": "Template-%s", "categoriaId": "%s"}
                """.formatted(UUID.randomUUID(), categoriaId));
        return crearYObtenerId("/api/v1/variantes", token, """
                {"sku": "SKU-%s", "templateId": "%s"}
                """.formatted(UUID.randomUUID(), templateId));
    }

    private String cuerpoPorVariante(String varianteId, int minimo, int objetivo) {
        return """
                {"varianteId": "%s", "cantidadMinima": %d, "cantidadObjetivo": %d}
                """.formatted(varianteId, minimo, objetivo);
    }

    private String cuerpoPorCategoria(String categoriaId, int minimo, int objetivo) {
        return """
                {"categoriaId": "%s", "cantidadMinima": %d, "cantidadObjetivo": %d}
                """.formatted(categoriaId, minimo, objetivo);
    }

    // ---- escenarios ----------------------------------------------------------

    @Test
    void crearUmbralPorVarianteDevuelve201() throws Exception {
        String token = loguearComoAdmin("umbral-crea-variante@ejemplo.com");
        String variante = crearVariante(token, crearCategoria(token));

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorVariante(variante, 3, 6)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.varianteId").value(variante))
                .andExpect(jsonPath("$.categoriaId").doesNotExist())
                .andExpect(jsonPath("$.cantidadMinima").value(3))
                .andExpect(jsonPath("$.cantidadObjetivo").value(6));
    }

    @Test
    void crearUmbralPorCategoriaDevuelve201() throws Exception {
        String token = loguearComoAdmin("umbral-crea-categoria@ejemplo.com");
        String categoria = crearCategoria(token);

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorCategoria(categoria, 2, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaId").value(categoria));
    }

    @Test
    void crearConVarianteYCategoriaALaVezDevuelve400() throws Exception {
        String token = loguearComoAdmin("umbral-ambos@ejemplo.com");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"varianteId": "%s", "categoriaId": "%s", "cantidadMinima": 3, "cantidadObjetivo": 6}
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearSinVarianteNiCategoriaDevuelve400() throws Exception {
        String token = loguearComoAdmin("umbral-ninguno@ejemplo.com");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidadMinima": 3, "cantidadObjetivo": 6}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConObjetivoNoMayorAlMinimoDevuelve400() throws Exception {
        String token = loguearComoAdmin("umbral-objetivo@ejemplo.com");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorCategoria(crearCategoria(token), 5, 5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConMinimoCeroDevuelve400() throws Exception {
        String token = loguearComoAdmin("umbral-minimo-cero@ejemplo.com");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorCategoria(crearCategoria(token), 0, 5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearConVarianteInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("umbral-variante-404@ejemplo.com");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorVariante(UUID.randomUUID().toString(), 3, 6)))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearUnSegundoUmbralParaLaMismaVarianteDevuelve409() throws Exception {
        String token = loguearComoAdmin("umbral-duplicado-variante@ejemplo.com");
        String variante = crearVariante(token, crearCategoria(token));
        crearYObtenerId(RUTA, token, cuerpoPorVariante(variante, 3, 6));

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorVariante(variante, 4, 8)))
                .andExpect(status().isConflict());
    }

    @Test
    void crearUnSegundoUmbralParaLaMismaCategoriaDevuelve409() throws Exception {
        String token = loguearComoAdmin("umbral-duplicado-categoria@ejemplo.com");
        String categoria = crearCategoria(token);
        crearYObtenerId(RUTA, token, cuerpoPorCategoria(categoria, 3, 6));

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorCategoria(categoria, 4, 8)))
                .andExpect(status().isConflict());
    }

    @Test
    void crearConCategoriaInexistenteDevuelve409PorLaFk() throws Exception {
        String token = loguearComoAdmin("umbral-categoria-fk@ejemplo.com");

        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorCategoria(UUID.randomUUID().toString(), 3, 6)))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerYListarDevuelvenElUmbralCreado() throws Exception {
        String token = loguearComoAdmin("umbral-consulta@ejemplo.com");
        String id = crearYObtenerId(RUTA, token, cuerpoPorCategoria(crearCategoria(token), 3, 6));

        mockMvc.perform(get(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get(RUTA).param("page", "0").param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void obtenerUnUmbralInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("umbral-obtener-404@ejemplo.com");

        mockMvc.perform(get(RUTA + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void modificarCambiaLasCantidadesYConservaElAlcance() throws Exception {
        String token = loguearComoAdmin("umbral-modifica@ejemplo.com");
        String categoria = crearCategoria(token);
        String id = crearYObtenerId(RUTA, token, cuerpoPorCategoria(categoria, 3, 6));

        mockMvc.perform(put(RUTA + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidadMinima": 8, "cantidadObjetivo": 12}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaId").value(categoria))
                .andExpect(jsonPath("$.cantidadMinima").value(8))
                .andExpect(jsonPath("$.cantidadObjetivo").value(12));
    }

    @Test
    void modificarConObjetivoNoMayorAlMinimoDevuelve400() throws Exception {
        String token = loguearComoAdmin("umbral-modifica-invalido@ejemplo.com");
        String id = crearYObtenerId(RUTA, token, cuerpoPorCategoria(crearCategoria(token), 3, 6));

        mockMvc.perform(put(RUTA + "/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cantidadMinima": 9, "cantidadObjetivo": 9}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarYLuegoObtenerDevuelve404() throws Exception {
        String token = loguearComoAdmin("umbral-elimina@ejemplo.com");
        String id = crearYObtenerId(RUTA, token, cuerpoPorCategoria(crearCategoria(token), 3, 6));

        mockMvc.perform(delete(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(RUTA + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarUnUmbralInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("umbral-elimina-404@ejemplo.com");

        mockMvc.perform(delete(RUTA + "/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioSinElScopeRecibe403() throws Exception {
        String token = loguearComoUsuarioComun("umbral-sin-scope@ejemplo.com");

        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoPorCategoria(UUID.randomUUID().toString(), 3, 6)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacionSeRechazaCon401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }
}
