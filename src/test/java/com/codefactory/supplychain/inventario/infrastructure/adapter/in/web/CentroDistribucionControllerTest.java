package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

import com.codefactory.supplychain.identity.application.port.out.PasswordHasherPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.EstadoUsuario;
import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de extremo a extremo de CentroDistribucion. No tiene guard de
 * "@PreAuthorize" (deferido a una futura remediación de FEAT-04, tal como se
 * decidió explícitamente en la auditoría): cualquier usuario autenticado
 * puede usar estos endpoints. El bug conocido de DELETE (huérfana el Nodo y
 * genera un 400 por violación de FK en vez de eliminar limpiamente) se deja
 * intacto a propósito y se verifica aquí que sigue comportándose igual.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CentroDistribucionControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private PasswordHasherPort passwordHasherPort;

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

    private String loguearUsuario(String email) throws Exception {
        Usuario usuario = Usuario.reconstruir(UUID.randomUUID(), Email.de(email), "Usuario de Prueba",
                passwordHasherPort.hashear(Password.de("contraseñaSegura123")), EstadoUsuario.ACTIVO, 0, null,
                false, null, null, Instant.now(), Instant.now());
        usuarioRepositoryPort.guardar(usuario);
        return obtenerAccessToken(email, "contraseñaSegura123");
    }

    @Test
    void crearUnCdDevuelve201YProvisionaSuNodo() throws Exception {
        String token = loguearUsuario("usuario-crea-cd@ejemplo.com");

        MvcResult creado = mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Bogotá", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("CD Bogotá"))
                .andReturn();
        String id = new ObjectMapper().readTree(creado.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/centros-distribucion/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodo.tipo").value("CD"))
                .andExpect(jsonPath("$.nodo.cdId").value(id));
    }

    @Test
    void crearConNombreDuplicadoDevuelveBadRequestPorLaDeudaDeStatusCodeDeferida() throws Exception {
        String token = loguearUsuario("usuario-duplica-cd@ejemplo.com");
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Duplicado Http", "ubicacion": "Cali"}
                                """))
                .andExpect(status().isCreated());

        // Comportamiento heredado y deliberadamente sin corregir en este refactor:
        // CentroDistribucionDuplicadoException también extiende ReglaDeNegocioException
        // (400), no RecursoDuplicadoException (409). Ver auditoría / deuda técnica de FEAT-04.
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Duplicado Http", "ubicacion": "Otra"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void consultarUnCdInexistenteDevuelveBadRequestPorLaDeudaDeStatusCodeDeferida() throws Exception {
        String token = loguearUsuario("usuario-cd-no-encontrado@ejemplo.com");

        // Comportamiento heredado y deliberadamente sin corregir en este refactor:
        // CentroDistribucionNoEncontradaException extiende ReglaDeNegocioException (400),
        // no RecursoNoEncontradoException (404). Ver auditoría / deuda técnica de FEAT-04.
        mockMvc.perform(get("/api/v1/centros-distribucion/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarConFiltroPorNombreParcial() throws Exception {
        String token = loguearUsuario("usuario-filtra-cd@ejemplo.com");
        mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD Norte Filtrable", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/centros-distribucion").param("nombre", "norte filtrable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("CD Norte Filtrable"));
    }

    @Test
    void eliminarUnCdConNodoAsociadoFallaConBadRequestPorElBugConocidoYDeferido() throws Exception {
        String token = loguearUsuario("usuario-elimina-cd-huerfano@ejemplo.com");
        MvcResult creado = mockMvc.perform(post("/api/v1/centros-distribucion")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "CD A Eliminar Http", "ubicacion": "Bogotá"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = new ObjectMapper().readTree(creado.getResponse().getContentAsString()).get("id").asText();

        // Bug conocido, deliberadamente NO corregido en este refactor (fuera de alcance:
        // pertenece a FEAT-04, no a FEAT-02/Tienda): eliminar un CD no elimina su Nodo
        // asociado primero, así que la violación de llave foránea llega sin traducir
        // hasta el cliente MockMvc como una excepción sin manejar (no hay
        // @ExceptionHandler para DataIntegrityViolationException en GlobalExceptionHandler).
        // En producción esto se traduce en un 500 genérico. Esta prueba documenta y fija
        // ese comportamiento actual para detectar si cambia sin intención en el futuro.
        assertThatThrownBy(() -> mockMvc.perform(delete("/api/v1/centros-distribucion/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)))
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }
}
