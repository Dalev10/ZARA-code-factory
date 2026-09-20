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
 * Prueba de extremo a extremo de BodegaTienda (rebuilt sobre Nodo). Cubre las
 * reglas nuevas: tienda debe existir (404), tienda debe estar activa (400),
 * una bodega por tienda (409), y el guard de "tiendas:administrar".
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class BodegaTiendaControllerTest {

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
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario de Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        usuario = usuarioRepositoryPort.guardar(usuario);
        UUID rolAdminId = rolRepositoryPort.buscarPorNombre("ADMIN").orElseThrow().getId();
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(usuario.getId(), rolAdminId)));
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    private String crearTienda(String token, String nombre) throws Exception {
        MvcResult creada = mockMvc.perform(post("/api/v1/tiendas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "%s", "ubicacion": "Bogotá"}
                                """.formatted(nombre)))
                .andExpect(status().isCreated())
                .andReturn();
        return new ObjectMapper().readTree(creada.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void registrarUnaBodegaParaUnaTiendaActivaDevuelve201() throws Exception {
        String token = loguearComoAdmin("admin-registra-bodega@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Con Bodega");

        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tiendaId").value(tiendaId));
    }

    @Test
    void registrarComoUsuarioComunSeRechazaCon403() throws Exception {
        String tokenAdmin = loguearComoAdmin("admin-crea-tienda-para-403@ejemplo.com");
        String tiendaId = crearTienda(tokenAdmin, "Tienda Para 403");

        Usuario comun = Usuario.reconstruir(UUID.randomUUID(), Email.de("no-admin-bodega@ejemplo.com"),
                "Usuario Común", passwordHasherPort.hashear(Password.de("contraseñaSegura123")),
                EstadoUsuario.ACTIVO, 0, null, false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(comun);
        String tokenComun = obtenerAccessToken("no-admin-bodega@ejemplo.com", "contraseñaSegura123");

        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComun)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrarConTiendaInexistenteDevuelve404() throws Exception {
        String token = loguearComoAdmin("admin-bodega-tienda-404@ejemplo.com");

        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarConTiendaInactivaDevuelve400() throws Exception {
        String token = loguearComoAdmin("admin-bodega-tienda-inactiva@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Inactiva Para Bodega");
        mockMvc.perform(delete("/api/v1/tiendas/" + tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrarUnaSegundaBodegaParaLaMismaTiendaDevuelve409() throws Exception {
        String token = loguearComoAdmin("admin-bodega-duplicada@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Con Bodega Duplicada");

        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarPorTiendaIdDevuelveLaBodegaEnriquecidaConDatosDeTienda() throws Exception {
        String token = loguearComoAdmin("admin-consulta-bodega@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Consultada");
        mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/bodegas-tienda/tienda/" + tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiendaId").value(tiendaId))
                .andExpect(jsonPath("$.tiendaNombre").value("Tienda Consultada"));
    }

    @Test
    void eliminarUnaBodegaSinInventarioDevuelve204() throws Exception {
        String token = loguearComoAdmin("admin-elimina-bodega@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Bodega A Eliminar");
        MvcResult registrada = mockMvc.perform(post("/api/v1/bodegas-tienda")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tiendaId": "%s"}
                                """.formatted(tiendaId)))
                .andExpect(status().isCreated())
                .andReturn();
        String bodegaId = new ObjectMapper().readTree(registrada.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/v1/bodegas-tienda/" + bodegaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/bodegas-tienda/" + bodegaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
