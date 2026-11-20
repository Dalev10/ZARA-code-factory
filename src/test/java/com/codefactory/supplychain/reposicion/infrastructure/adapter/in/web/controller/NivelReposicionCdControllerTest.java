package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort;
import com.codefactory.supplychain.reposicion.application.port.out.ResolverNivelPort.NivelAplicable;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HD-04 de extremo a extremo, contra PostgreSQL real: CRUD de los niveles de reposición al CD,
 * restricciones de V35 y precedencia del resolver que consume HU-43.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("NivelReposicionCdController (HD-04)")
class NivelReposicionCdControllerTest {

    private static final String RUTA = "/api/v1/niveles-reposicion-cd";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private ResolverNivelPort resolver;

    private String admin;
    private UUID categoriaId;
    private UUID varianteId;

    @BeforeEach
    void setUp() {
        admin = usuarios.conScopes(Set.of("reposicion:administrar")).bearer();
        categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        varianteId = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", varianteId, templateId,
                "SKU-" + varianteId);
    }

    @Test
    @DisplayName("""
            Escenario: precedencia variante → categoría
              Dado un nivel por categoría (10/30) y otro por la variante (5/15)
              Cuando se resuelve el nivel de la variante
              Entonces aplica el de la variante
              Y al eliminarlo aplica el de su categoría""")
    void elNivelDeLaVarianteTienePrecedencia() throws Exception {
        crear(null, categoriaId, 10, 30).andExpect(status().isCreated());
        String idVariante = id(crear(varianteId, null, 5, 15).andExpect(status().isCreated()));

        assertThat(resolver.resolver(UUID.randomUUID(), varianteId)).contains(new NivelAplicable(5, 15));

        mockMvc.perform(delete(RUTA + "/{id}", idVariante).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        assertThat(resolver.resolver(UUID.randomUUID(), varianteId)).contains(new NivelAplicable(10, 30));
    }

    @Test
    @DisplayName("Consultar, listar y modificar cantidades; el alcance no cambia")
    void consultarListarYModificar() throws Exception {
        String id = id(crear(varianteId, null, 5, 15).andExpect(status().isCreated()));

        mockMvc.perform(get(RUTA + "/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.varianteId").value(varianteId.toString()))
                .andExpect(jsonPath("$.categoriaId").doesNotExist());
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(id)).exists());
        mockMvc.perform(put(RUTA + "/{id}", id).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadMinima\": 8, \"cantidadObjetivo\": 20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadMinima").value(8))
                .andExpect(jsonPath("$.cantidadObjetivo").value(20))
                .andExpect(jsonPath("$.varianteId").value(varianteId.toString()));
    }

    @Test
    @DisplayName("Reglas: uno por alcance (409), alcance excluyente y cantidades (400), variante inexistente (404)")
    void reglasDeCreacion() throws Exception {
        crear(varianteId, null, 5, 15).andExpect(status().isCreated());

        crear(varianteId, null, 6, 16).andExpect(status().isConflict());
        crear(varianteId, categoriaId, 5, 15).andExpect(status().isBadRequest());
        crear(null, null, 5, 15).andExpect(status().isBadRequest());
        crear(null, categoriaId, 5, 5).andExpect(status().isBadRequest());
        crear(UUID.randomUUID(), null, 5, 15).andExpect(status().isNotFound());
        crear(null, UUID.randomUUID(), 5, 15).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Un nivel inexistente responde 404 y sin el scope reposicion:administrar, 403")
    void inexistenteYSeguridad() throws Exception {
        mockMvc.perform(get(RUTA + "/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(RUTA + "/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, usuarios.conScopes(Set.of()).bearer()))
                .andExpect(status().isForbidden());
    }

    private ResultActions crear(UUID variante, UUID categoria, int minimo, int objetivo) throws Exception {
        String cuerpo = "{\"varianteId\": %s, \"categoriaId\": %s, \"cantidadMinima\": %d, \"cantidadObjetivo\": %d}"
                .formatted(comillas(variante), comillas(categoria), minimo, objetivo);
        return mockMvc.perform(post(RUTA).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String comillas(UUID id) {
        return id == null ? "null" : "\"" + id + "\"";
    }

    private static String id(ResultActions respuesta) throws Exception {
        return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
