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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo del CRUD de Tienda (FEAT-02), incluyendo el
 * guard de autorización por scope ("tiendas:administrar") de HU-11.
 *
 * @ActiveProfiles("test") carga application-test.yml (secreto JWT fijo de test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class TiendaControllerTest {

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
    void listarPaginaLosResultados() throws Exception {
        String token = loguearComoAdmin("admin-pagina-tiendas@ejemplo.com");
        for (String nombre : new String[] {"Tienda Pag 1", "Tienda Pag 2", "Tienda Pag 3"}) {
            mockMvc.perform(post("/api/v1/tiendas")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nombre": "%s", "ubicacion": "Bogotá"}
                                    """.formatted(nombre)))
                    .andExpect(status().isCreated());
        }

        // No se asume que la tabla esté vacía (otros tests de esta misma clase
        // también crean tiendas y no hay rollback entre métodos): solo se
        // verifica que el tamaño de página se respeta y que el conteo total
        // incluye, al menos, las 3 tiendas recién creadas.
        mockMvc.perform(get("/api/v1/tiendas").param("page", "0").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void crearComoAdminDevuelve201ConEstadoActiva() throws Exception {
        String token = loguearComoAdmin("admin-crea-tienda@ejemplo.com");

        mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Centro", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Tienda Centro"))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void crearComoUsuarioComunSeRechazaCon403() throws Exception {
        String token = loguearComoUsuarioComun("no-admin-crea-tienda@ejemplo.com");

        mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Rechazada", "ubicacion": "Test"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearSinAutenticacionSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/tiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Sin Auth", "ubicacion": "Test"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void crearConNombreDuplicadoDevuelve409() throws Exception {
        String token = loguearComoAdmin("admin-duplica-tienda@ejemplo.com");
        mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Duplicada Http", "ubicacion": "Test"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Duplicada Http", "ubicacion": "Otra"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerUnaTiendaInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("admin-tienda-404@ejemplo.com");

        mockMvc.perform(get("/api/v1/tiendas/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarUnaTiendaExistente() throws Exception {
        String token = loguearComoAdmin("admin-actualiza-tienda@ejemplo.com");
        MvcResult creada = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda A Actualizar", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/tiendas/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Actualizada", "ubicacion": "Medellín"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tienda Actualizada"))
                .andExpect(jsonPath("$.ubicacion").value("Medellín"));
    }

    @Test
    void desactivarUnaTiendaDevuelve204YCambiaSuEstado() throws Exception {
        String token = loguearComoAdmin("admin-desactiva-tienda@ejemplo.com");
        MvcResult creada = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda A Desactivar", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/v1/tiendas/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tiendas/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVA"));
    }

    @Test
    void reactivarUnaTiendaDesactivadaLaVuelveActiva() throws Exception {
        String token = loguearComoAdmin("admin-reactiva-tienda@ejemplo.com");
        MvcResult creada = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda A Reactivar", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(delete("/api/v1/tiendas/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/v1/tiendas/" + id + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    @Test
    void reactivarUnaTiendaYaActivaEsIdempotente() throws Exception {
        String token = loguearComoAdmin("admin-reactiva-tienda-idempotente@ejemplo.com");
        MvcResult creada = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Tienda Ya Activa", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/tiendas/" + id + "/activar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }
}
