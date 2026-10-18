package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web;

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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de aceptación de HU-30 (consultas de inventario y kardex) de
 * extremo a extremo, contra PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
class ConsultaInventarioControllerTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsuariosDePrueba usuarios;

    private UUID tiendaId;
    private UUID almacenId;
    private UsuarioDePrueba vendedor;

    @BeforeEach
    void setUp() {
        tiendaId = crearTienda();
        almacenId = crearNodoDeTienda(tiendaId);
        vendedor = usuarios.conRol("VENDEDOR", tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: el stock de un nodo se consulta con paginación
              Dado un Almacén con la variante X con 5 unidades
              Cuando un vendedor de la tienda consulta el inventario del Almacén
              Entonces la respuesta es 200 con a_la_mano 5 para X""")
    void elStockDeUnNodoSeConsulta() throws Exception {
        String x = crearVarianteCargada(almacenId, 5);

        consultarStock(almacenId, vendedor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].aLaMano").value(5))
                .andExpect(jsonPath("$.content[0].varianteId").value(varianteId(x).toString()));
    }

    @Test
    @DisplayName("""
            Escenario: usuario de tienda no ve nodos ajenos
              Dado un usuario asignado solo a la tienda T1
              Cuando consulta el inventario del Almacén de T2
              Entonces la respuesta es 404""")
    void usuarioDeTiendaNoVeNodosAjenos() throws Exception {
        UUID tiendaAjena = crearTienda();
        UUID almacenAjeno = crearNodoDeTienda(tiendaAjena);
        crearVarianteCargada(almacenAjeno, 3);

        consultarStock(almacenAjeno, vendedor).andExpect(status().isNotFound());
        consultarKardex(almacenAjeno, vendedor).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("""
            Escenario: la consulta por variante solo muestra los nodos visibles
              Dado la variante X cargada en el Almacén de T1 y en el Almacén de T2
              Cuando un vendedor solo asignado a T1 consulta la variante X
              Entonces solo aparece el Almacén de T1""")
    void laConsultaPorVarianteSoloMuestraNodosVisibles() throws Exception {
        UUID tiendaAjena = crearTienda();
        UUID almacenAjeno = crearNodoDeTienda(tiendaAjena);
        String x = crearVarianteCargada(almacenId, 4);
        cargar(almacenAjeno, x, 9);

        mockMvc.perform(get("/api/v1/variantes/{varianteId}/inventario", varianteId(x))
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nodoId").value(almacenId.toString()))
                .andExpect(jsonPath("$[0].aLaMano").value(4));
    }

    @Test
    @DisplayName("""
            Escenario: el kardex refleja cada movimiento en orden
              Dado un Almacén con una carga inicial, dos ventas y un ajuste
              Cuando se consulta el kardex
              Entonces aparecen 4 movimientos ordenados por ocurrido_en""")
    void elKardexReflejaCadaMovimientoEnOrden() throws Exception {
        String x = crearVarianteCargada(almacenId, 10);
        fijarFecha(almacenId, "CARGA_INICIAL", Instant.parse("2026-10-18T09:00:00Z"));
        registrarMovimiento(almacenId, x, "VENTA", -1, Instant.parse("2026-10-18T10:00:00Z"));
        registrarMovimiento(almacenId, x, "VENTA", -2, Instant.parse("2026-10-18T11:00:00Z"));
        ajustar(almacenId, x, -1, "MERMA");
        fijarFecha(almacenId, "AJUSTE", Instant.parse("2026-10-18T12:00:00Z"));

        consultarKardex(almacenId, vendedor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].tipo").value("CARGA_INICIAL"))
                .andExpect(jsonPath("$.content[1].tipo").value("VENTA"))
                .andExpect(jsonPath("$.content[3].tipo").value("AJUSTE"));
    }

    @Test
    @DisplayName("""
            Escenario: el kardex se filtra por tipo
              Dado un Almacén con una carga inicial y dos ventas
              Cuando se consulta el kardex filtrado por VENTA
              Entonces aparecen 2 movimientos""")
    void elKardexSeFiltraPorTipo() throws Exception {
        String x = crearVarianteCargada(almacenId, 10);
        registrarMovimiento(almacenId, x, "VENTA", -1, Instant.parse("2026-10-18T10:00:00Z"));
        registrarMovimiento(almacenId, x, "VENTA", -2, Instant.parse("2026-10-18T11:00:00Z"));

        mockMvc.perform(get("/api/v1/nodos/{nodoId}/inventario/movimientos", almacenId)
                        .param("tipo", "VENTA")
                        .header(HttpHeaders.AUTHORIZATION, vendedor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("""
            Escenario: sin el scope de consulta la operación responde 403
              Dado un usuario con el scope ventas:registrar pero sin inventario:consultar
              Cuando consulta el inventario del Almacén de su tienda
              Entonces la respuesta es 403""")
    void sinElScopeDeConsultaResponde403() throws Exception {
        UsuarioDePrueba sinConsulta = usuarios.conScopes(Set.of("ventas:registrar"), tiendaId);

        consultarStock(almacenId, sinConsulta).andExpect(status().isForbidden());
    }

    private ResultActions consultarStock(UUID nodoId, UsuarioDePrueba usuario) throws Exception {
        return mockMvc.perform(get("/api/v1/nodos/{nodoId}/inventario", nodoId)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
    }

    private ResultActions consultarKardex(UUID nodoId, UsuarioDePrueba usuario) throws Exception {
        return mockMvc.perform(get("/api/v1/nodos/{nodoId}/inventario/movimientos", nodoId)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
    }

    private void cargar(UUID nodoId, String sku, int cantidad) throws Exception {
        UsuarioDePrueba supervisor = usuarios.conRol("SUPERVISOR_TIENDA", tiendaDe(nodoId));
        mockMvc.perform(post("/api/v1/nodos/{nodoId}/inventario/carga-inicial", nodoId)
                        .header(HttpHeaders.AUTHORIZATION, supervisor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"sku\": \"%s\", \"cantidad\": %d}]}".formatted(sku, cantidad)))
                .andExpect(status().isCreated());
    }

    private void ajustar(UUID nodoId, String sku, int delta, String motivo) throws Exception {
        UsuarioDePrueba supervisor = usuarios.conRol("SUPERVISOR_TIENDA", tiendaDe(nodoId));
        mockMvc.perform(post("/api/v1/nodos/{nodoId}/inventario/ajustes", nodoId)
                        .header(HttpHeaders.AUTHORIZATION, supervisor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": \"%s\", \"delta\": %d, \"motivo\": \"%s\"}"
                                .formatted(sku, delta, motivo)))
                .andExpect(status().isCreated());
    }

    private void fijarFecha(UUID nodoId, String tipo, Instant ocurridoEn) {
        jdbc.update("UPDATE movimiento_inventario SET ocurrido_en = ? WHERE nodo_id = ? AND tipo = ?",
                Timestamp.from(ocurridoEn), nodoId, tipo);
    }

    private String crearVarianteCargada(UUID nodoId, int cantidad) throws Exception {
        String sku = crearVariante();
        cargar(nodoId, sku, cantidad);
        return sku;
    }

    private void registrarMovimiento(UUID nodoId, String sku, String tipo, int cantidad, Instant ocurridoEn) {
        jdbc.update("""
                INSERT INTO movimiento_inventario (id, nodo_id, variante_id, tipo, cantidad, ocurrido_en)
                VALUES (?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID(), nodoId, varianteId(sku), tipo, cantidad, Timestamp.from(ocurridoEn));
    }

    private UUID varianteId(String sku) {
        return jdbc.queryForObject("SELECT id FROM variante WHERE sku = ?", UUID.class, sku);
    }

    private UUID tiendaDe(UUID nodoId) {
        return jdbc.queryForObject("SELECT tienda_id FROM nodo WHERE id = ?", UUID.class, nodoId);
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                id, "Tienda Consulta " + id);
        return id;
    }

    private UUID crearNodoDeTienda(UUID tienda) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", id, tienda);
        return id;
    }

    private String crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template " + templateId);
        String sku = "SKU-" + UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)",
                UUID.randomUUID(), templateId, sku);
        return sku;
    }
}
