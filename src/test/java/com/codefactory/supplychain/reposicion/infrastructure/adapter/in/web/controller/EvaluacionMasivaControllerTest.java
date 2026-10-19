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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de HU-50 (evaluación masiva de reposición interna) de extremo a extremo,
 * incluido el guard por scope {@code reposicion:administrar} (D-57).
 *
 * Módulo: reposicion — Reposición interna.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("EvaluacionMasivaController (HU-50)")
class EvaluacionMasivaControllerTest {

    private static final String RUTA_EVALUAR_TODAS = "/api/v1/reposicion-interna/evaluar";
    private static final String RUTA_EVALUAR_TIENDA = "/api/v1/reposicion-interna/tiendas/{tiendaId}/evaluar";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;

    private UsuarioDePrueba admin;

    @BeforeEach
    void setUp() {
        admin = usuarios.conRol("ADMIN");
    }

    @Test
    @DisplayName("""
            Escenario: tras subir un mínimo, se reponen los Almacenes afectados
              Dado un Almacén con 2 unidades y una Bodega con 10, sin umbral
              Y se crea un umbral de mínimo 3 y objetivo 6 (crear un umbral no genera eventos, D-57)
              Cuando se ejecuta la evaluación masiva
              Entonces se ejecuta un traspaso de 4 unidades
              Y el Almacén queda en 6 y la Bodega en 6""")
    void trasSubirUnMinimoSeReponenLosAlmacenes() throws Exception {
        UUID tiendaId = crearTienda();
        UUID almacenId = crearNodo(tiendaId, "ALMACEN");
        UUID bodegaId = crearNodo(tiendaId, "BODEGA_TIENDA");
        UUID varianteId = crearVariante();
        inventario.cargar(almacenId, varianteId, 2, null);
        inventario.cargar(bodegaId, varianteId, 10, null);
        crearUmbral(varianteId, 3, 6);

        mockMvc.perform(post(RUTA_EVALUAR_TODAS).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traspasoEjecutados").value(1))
                .andExpect(jsonPath("$.traspasos[0].cantidad").value(4))
                .andExpect(jsonPath("$.traspasos[0].origen").value("AUTOMATICO"));

        assertThat(aLaMano(almacenId, varianteId)).isEqualTo(6);
        assertThat(aLaMano(bodegaId, varianteId)).isEqualTo(6);
    }

    @Test
    @DisplayName("Una tienda sin inventario responde 200 con cero traspasos")
    void tiendaSinInventarioNoGeneraTraspasos() throws Exception {
        mockMvc.perform(post(RUTA_EVALUAR_TIENDA, crearTienda()).header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traspasoEjecutados").value(0))
                .andExpect(jsonPath("$.traspasos.length()").value(0));
    }

    @Test
    @DisplayName("Sin token responde 401; sin el scope reposicion:administrar, 403")
    void seguridad() throws Exception {
        mockMvc.perform(post(RUTA_EVALUAR_TODAS)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(RUTA_EVALUAR_TIENDA, UUID.randomUUID())).andExpect(status().isUnauthorized());

        String sinScope = usuarios.conScopes(Set.of()).bearer();
        mockMvc.perform(post(RUTA_EVALUAR_TODAS).header(HttpHeaders.AUTHORIZATION, sinScope))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(RUTA_EVALUAR_TIENDA, UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, sinScope))
                .andExpect(status().isForbidden());
    }

    private void crearUmbral(UUID varianteId, int minimo, int objetivo) throws Exception {
        mockMvc.perform(post("/api/v1/umbrales-reposicion")
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"varianteId\": \"%s\", \"cantidadMinima\": %d, \"cantidadObjetivo\": %d}"
                                .formatted(varianteId, minimo, objetivo)))
                .andExpect(status().isCreated());
    }

    private int aLaMano(UUID nodoId, UUID varianteId) {
        return jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, nodoId, varianteId);
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", id, "Tienda " + id);
        return id;
    }

    private UUID crearNodo(UUID tiendaId, String tipo) {
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
