package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import com.codefactory.supplychain.soporte.UsuariosDePrueba.UsuarioDePrueba;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de HU-49 (traspaso manual y su consulta) de extremo a extremo, contra
 * PostgreSQL real: nunca deja la Bodega en negativo, registra al usuario del token y
 * solo opera sobre las tiendas asignadas.
 *
 * Módulo: reposicion — Reposición interna.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("TraspasoInternoController (HU-49)")
class TraspasoInternoControllerTest {

    private static final String RUTA_TIENDA = "/api/v1/tiendas/{tiendaId}/traspasos-internos";
    private static final String RUTA_DETALLE = "/api/v1/traspasos-internos/{id}";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;

    private UUID tiendaId;
    private UUID bodegaId;
    private UUID almacenId;
    private UUID varianteId;
    private UsuarioDePrueba operario;

    @BeforeEach
    void setUp() {
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda " + tiendaId);
        bodegaId = crearNodo("BODEGA_TIENDA");
        almacenId = crearNodo("ALMACEN");
        varianteId = crearVariante();
        operario = usuarios.conScopes(Set.of("reposicion:administrar"), tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: traspaso manual registra el movimiento y su autor
              Dada una Bodega con 10 unidades de la variante X
              Cuando el operario registra un traspaso manual de 4 unidades
              Entonces la respuesta es 201 con origen MANUAL y el operario como registrador
              Y la Bodega queda en 6 y el Almacén en 4""")
    void traspasoManualMueveStockYRegistraAutor() throws Exception {
        inventario.cargar(bodegaId, varianteId, 10, null);

        registrar(operario, tiendaId, varianteId, 4)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andExpect(jsonPath("$.cantidad").value(4))
                .andExpect(jsonPath("$.registradoPor").value(operario.id().toString()));

        assertThat(aLaMano(bodegaId)).isEqualTo(6);
        assertThat(aLaMano(almacenId)).isEqualTo(4);
    }

    @Test
    @DisplayName("""
            Escenario: el traspaso manual nunca deja la Bodega en negativo
              Dada una Bodega con 2 unidades de la variante X
              Cuando el operario registra un traspaso de 3 unidades
              Entonces la respuesta es 409 y la Bodega sigue en 2""")
    void bodegaInsuficienteResponde409() throws Exception {
        inventario.cargar(bodegaId, varianteId, 2, null);

        registrar(operario, tiendaId, varianteId, 3).andExpect(status().isConflict());

        assertThat(aLaMano(bodegaId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM traspaso_interno WHERE tienda_id = ?",
                Integer.class, tiendaId)).isZero();
    }

    @Test
    @DisplayName("Una tienda no asignada responde 403 al registrar y al listar; el detalle responde 404")
    void tiendaAjenaNoSeOpera() throws Exception {
        inventario.cargar(bodegaId, varianteId, 5, null);
        String id = idDe(registrar(operario, tiendaId, varianteId, 1));
        UUID otraTienda = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Cali')", otraTienda, "Tienda " + otraTienda);
        UsuarioDePrueba ajeno = usuarios.conScopes(Set.of("reposicion:administrar"), otraTienda);

        registrar(ajeno, tiendaId, varianteId, 1).andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA_TIENDA, tiendaId).header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA_DETALLE, id).header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El historial y el detalle devuelven los traspasos de la tienda")
    void historialYDetalle() throws Exception {
        inventario.cargar(bodegaId, varianteId, 5, null);
        String id = idDe(registrar(operario, tiendaId, varianteId, 2));

        mockMvc.perform(get(RUTA_TIENDA, tiendaId).header(HttpHeaders.AUTHORIZATION, operario.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get(RUTA_DETALLE, id).header(HttpHeaders.AUTHORIZATION, operario.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(2));
        mockMvc.perform(get(RUTA_DETALLE, UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, operario.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Una variante inexistente responde 404")
    void varianteInexistente() throws Exception {
        registrar(operario, tiendaId, UUID.randomUUID(), 1).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin token responde 401; sin el scope reposicion:administrar, 403")
    void seguridad() throws Exception {
        mockMvc.perform(post(RUTA_TIENDA, tiendaId).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(varianteId, 1)))
                .andExpect(status().isUnauthorized());
        registrar(usuarios.conScopes(Set.of(), tiendaId), tiendaId, varianteId, 1)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una cantidad no positiva o sin variante responde 400")
    void validacionDeEntrada() throws Exception {
        registrar(operario, tiendaId, varianteId, 0).andExpect(status().isBadRequest());
        mockMvc.perform(post(RUTA_TIENDA, tiendaId)
                        .header(HttpHeaders.AUTHORIZATION, operario.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\": 5}"))
                .andExpect(status().isBadRequest());
    }

    private ResultActions registrar(UsuarioDePrueba usuario, UUID tienda, UUID variante, int cantidad)
            throws Exception {
        return mockMvc.perform(post(RUTA_TIENDA, tienda)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(variante, cantidad)));
    }

    private static String cuerpo(UUID variante, int cantidad) {
        return "{\"varianteId\": \"%s\", \"cantidad\": %d}".formatted(variante, cantidad);
    }

    private static String idDe(ResultActions respuesta) throws Exception {
        String json = respuesta.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.id");
    }

    private int aLaMano(UUID nodoId) {
        return jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, nodoId, varianteId);
    }

    private UUID crearNodo(String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, ?, ?)", id, tipo, tiendaId);
        return id;
    }

    private UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", id, templateId, "SKU-" + id);
        return id;
    }
}
