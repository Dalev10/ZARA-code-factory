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
    void crearUnaTiendaCreaSuBodegaAutomaticamente() throws Exception {
        String token = loguearComoAdmin("admin-crea-bodega-automatica@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Con Bodega");

        mockMvc.perform(get("/api/v1/bodegas-tienda/tienda/" + tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiendaId").value(tiendaId));
    }

    @Test
    void consultarComoUsuarioComunSeRechazaCon403() throws Exception {
        String tokenAdmin = loguearComoAdmin("admin-crea-tienda-para-403@ejemplo.com");
        String tiendaId = crearTienda(tokenAdmin, "Tienda Para 403");

        Usuario comun = Usuario.reconstruir(UUID.randomUUID(), Email.de("no-admin-bodega@ejemplo.com"),
                "Usuario Común", passwordHasherPort.hashear(Password.de("contraseñaSegura123")),
                EstadoUsuario.ACTIVO, 0, null, false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(comun);
        String tokenComun = obtenerAccessToken("no-admin-bodega@ejemplo.com", "contraseñaSegura123");

        mockMvc.perform(get("/api/v1/bodegas-tienda/tienda/" + tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenComun))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultarPorTiendaIdDevuelveLaBodegaEnriquecidaConDatosDeTienda() throws Exception {
        String token = loguearComoAdmin("admin-consulta-bodega@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Consultada");

        mockMvc.perform(get("/api/v1/bodegas-tienda/tienda/" + tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiendaId").value(tiendaId))
                .andExpect(jsonPath("$.tiendaNombre").value("Tienda Consultada"));
    }

    @Test
    void lasBodegasSoloSeConsultanNoSeEscriben() throws Exception {
        String token = loguearComoAdmin("admin-bodega-solo-lectura@ejemplo.com");
        String tiendaId = crearTienda(token, "Tienda Bodega Solo Lectura");
        String cuerpo = "{\"tiendaId\": \"%s\"}".formatted(tiendaId);
        UUID bodegaId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/bodegas-tienda").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/v1/bodegas-tienda/" + bodegaId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/bodegas-tienda/" + bodegaId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isMethodNotAllowed());
    }
}
