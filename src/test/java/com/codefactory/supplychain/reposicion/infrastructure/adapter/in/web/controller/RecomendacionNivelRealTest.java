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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de HU-43 con las piezas reales de develop, sin dobles: el nivel sale de
 * {@code nivel_reposicion_cd} y el planificador tiene el rol sembrado por V34 (HD-03).
 *
 * Módulo: reposicion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("Recomendaciones con el nivel real de HD-04")
class RecomendacionNivelRealTest {

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
    private UUID categoriaId;
    private UUID varianteId;
    private String sku;
    private UsuarioDePrueba planificador;

    @BeforeEach
    void setUp() {
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda " + tiendaId);
        bodegaId = crearNodoTienda("BODEGA_TIENDA");
        almacenId = crearNodoTienda("ALMACEN");
        crearVariante();
        planificador = usuarios.conRol("PLANIFICADOR_ABASTECIMIENTO", tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: Recomendación por consumo de la Bodega_Tienda (nivel por variante en V35)
              Dado que el pronosticado de una Bodega_Tienda para un SKU cae bajo el mínimo del nivel configurado
              Cuando el sistema evalúa la reposición
              Entonces crea una Recomendación en estado BORRADOR
              Y la cantidad_sugerida es el objetivo menos el pronosticado""")
    void recomendacionConNivelPorVariante() throws Exception {
        inventario.cargar(bodegaId, varianteId, 3, null);
        crearNivel("varianteId", varianteId, 5, 15);

        evaluar()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recomendacionesCreadas").value(1))
                .andExpect(jsonPath("$.recomendaciones[0].estado").value("BORRADOR"))
                .andExpect(jsonPath("$.recomendaciones[0].cantidadSugerida").value(12));
    }

    @Test
    @DisplayName("""
            Escenario: Recomendación por consumo de la Bodega_Tienda (nivel por categoría en V35)
              Dado que el SKU no tiene nivel propio pero su categoría sí
              Cuando el sistema evalúa la reposición
              Entonces usa el nivel de la categoría""")
    void recomendacionConNivelPorCategoria() throws Exception {
        inventario.cargar(bodegaId, varianteId, 2, null);
        crearNivel("categoriaId", categoriaId, 4, 9);

        evaluar().andExpect(jsonPath("$.recomendaciones[0].cantidadSugerida").value(7));
    }

    @Test
    @DisplayName("""
            Escenario: Recomendación por consumo de la Bodega_Tienda (precedencia variante → categoría)
              Dado que el SKU tiene nivel propio y su categoría también
              Cuando el sistema evalúa la reposición
              Entonces manda el nivel de la variante""")
    void nivelDeVarianteGanaAlDeCategoria() throws Exception {
        inventario.cargar(bodegaId, varianteId, 2, null);
        crearNivel("categoriaId", categoriaId, 4, 9);
        crearNivel("varianteId", varianteId, 5, 20);

        evaluar().andExpect(jsonPath("$.recomendaciones[0].cantidadSugerida").value(18));
    }

    @Test
    @DisplayName("""
            Escenario: Sin nivel configurado (tabla nivel_reposicion_cd vacía para el SKU)
              Dado que el SKU no tiene nivel de reposición
              Cuando el sistema evalúa
              Entonces no genera Recomendación""")
    void sinNivelEnLaTabla() throws Exception {
        inventario.cargar(bodegaId, varianteId, 1, null);

        evaluar().andExpect(jsonPath("$.recomendacionesCreadas").value(0));
    }

    @Test
    @DisplayName("""
            Escenario: Tiempo real (por una venta)
              Dado una venta que vacía el Almacén y el traspaso automático deja la Bodega bajo el mínimo
              Cuando se recibe la respuesta
              Entonces un GET inmediato ya muestra la Recomendación""")
    void tiempoRealPorVenta() throws Exception {
        inventario.cargar(almacenId, varianteId, 2, null);
        inventario.cargar(bodegaId, varianteId, 10, null);
        crearUmbralInterno(2, 5);
        crearNivel("varianteId", varianteId, 8, 12);

        UsuarioDePrueba vendedor = usuarios.conRol("VENDEDOR", tiendaId);
        mockMvc.perform(post("/api/v1/ventas")
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transaccionId": "%s", "tiendaId": "%s", "forzarSinStock": false,
                                 "items": [{"sku": "%s", "cantidad": 2, "precioUnitario": 49.90}]}"""
                                .formatted(UUID.randomUUID(), tiendaId, sku)))
                .andExpect(status().isCreated());

        // La venta dispara el traspaso Bodega → Almacén (@Order 20), y su TRASPASO_SALIDA, la recomendación (@Order 30).
        int bodega = jdbc.queryForObject("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, bodegaId, varianteId);
        assertThat(bodega).isLessThan(8);

        mockMvc.perform(get("/api/v1/recomendaciones")
                        .param("tiendaId", tiendaId.toString())
                        .param("estado", "BORRADOR")
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].cantidadSugerida").value(12 - bodega));
    }

    // -------------------------------------------------------------------------

    private ResultActions evaluar() throws Exception {
        return mockMvc.perform(post("/api/v1/recomendaciones/evaluar")
                .param("tiendaId", tiendaId.toString())
                .header(HttpHeaders.AUTHORIZATION, planificador.bearer()));
    }

    private void crearNivel(String campo, UUID id, int minimo, int objetivo) throws Exception {
        mockMvc.perform(post("/api/v1/niveles-reposicion-cd")
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"%s\": \"%s\", \"cantidadMinima\": %d, \"cantidadObjetivo\": %d}"
                                .formatted(campo, id, minimo, objetivo)))
                .andExpect(status().isCreated());
    }

    private void crearUmbralInterno(int minimo, int objetivo) throws Exception {
        mockMvc.perform(post("/api/v1/umbrales-reposicion")
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"varianteId\": \"%s\", \"cantidadMinima\": %d, \"cantidadObjetivo\": %d}"
                                .formatted(varianteId, minimo, objetivo)))
                .andExpect(status().isCreated());
    }

    private UUID crearNodoTienda(String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, ?, ?)", id, tipo, tiendaId);
        return id;
    }

    private void crearVariante() {
        categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        varianteId = UUID.randomUUID();
        sku = "SKU-" + varianteId;
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", varianteId, templateId, sku);
    }
}
