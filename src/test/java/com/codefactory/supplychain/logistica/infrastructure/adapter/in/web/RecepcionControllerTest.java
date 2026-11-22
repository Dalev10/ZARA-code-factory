package com.codefactory.supplychain.logistica.infrastructure.adapter.in.web;

import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.Comando;
import com.codefactory.supplychain.logistica.application.port.in.RegistrarRecepcionUseCase.ResultadoRecepcion;
import com.codefactory.supplychain.reposicion.api.OrdenParaDespacho;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import com.codefactory.supplychain.shared.security.UsuarioActual;
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
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-49 de extremo a extremo contra PostgreSQL real: la orden se crea, se despacha y se pone
 * EN_TRANSITO con los contratos reales de reposición e inventario.
 * <p>
 * La parada y su ciclo se insertan con SQL sobre las tablas de V1, porque HU-46 a HU-48
 * (ciclo, viaje y parada) todavía no tienen código. Cuando lleguen, estos datos pueden
 * crearse con sus casos de uso.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("Recepción de una parada (HU-49)")
class RecepcionControllerTest {

    private static final String RECEPCION = "recepcion:registrar";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;
    @Autowired private CrearOrdenUseCase crearOrden;
    @Autowired private TransicionesOrden transiciones;
    @Autowired private MovimientosAbastecimiento movimientos;
    @Autowired private RegistrarRecepcionUseCase registrarRecepcion;
    @Autowired private TransactionTemplate tx;

    private UUID tiendaId;
    private UUID bodegaId;
    private UUID varianteId;
    private UUID cdId;
    private UsuarioDePrueba receptor;

    @BeforeEach
    void setUp() {
        tiendaId = crearTienda();
        bodegaId = nodoDeTienda(tiendaId);
        varianteId = crearVariante();
        cdId = cdConStock(30);
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, 1)",
                tiendaId, cdId);
        receptor = usuarios.conScopes(Set.of(RECEPCION), tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: Recepción total
              Dado que lo recibido cubre lo solicitado de la orden
              Cuando registro la recepción
              Entonces la orden pasa a RECIBIDA_TOTAL y luego a CERRADA
              Y el a_la_mano de la Bodega aumenta y su entrante disminuye""")
    void recepcionTotal() throws Exception {
        UUID ordenId = crearOrden(10);
        assertThat(entranteBodega()).isEqualTo(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");

        recibir(parada, 10, receptor)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadRecibida").value(10))
                .andExpect(jsonPath("$.cantidadDespachada").value(10))
                .andExpect(jsonPath("$.discrepancia").value(0))
                .andExpect(jsonPath("$.registradoPor").value(receptor.id().toString()))
                .andExpect(jsonPath("$.estadoOrden").value("CERRADA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(0));

        assertThat(aLaManoBodega()).isEqualTo(10);
        assertThat(entranteBodega()).isZero();
        assertThat(estadoOrden(ordenId)).isEqualTo("CERRADA");
        verificarInvariantes(ordenId);
    }

    @Test
    @DisplayName("""
            Escenario: Recepción parcial
              Dado que recibo menos de lo despachado
              Cuando registro la recepción
              Entonces la orden pasa a RECIBIDA_PARCIAL y luego a REABIERTA
              Y la discrepancia queda registrada como pérdida en tránsito
              Y el pendiente se recalcula como solicitado menos recibido acumulado""")
    void recepcionParcial() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");

        recibir(parada, 7, receptor)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discrepancia").value(3))
                .andExpect(jsonPath("$.estadoOrden").value("REABIERTA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(3));

        assertThat(jdbc.queryForObject("SELECT discrepancia FROM recepcion WHERE viaje_parada_id = ?",
                Integer.class, parada.id())).isEqualTo(3);
        assertThat(aLaManoBodega()).isEqualTo(7);
        // La pérdida en tránsito no deja movimiento de kardex (D-90): solo la RECEPCION de lo que llegó.
        assertThat(kardexBodega()).containsExactly(Map.of("tipo", "RECEPCION", "cantidad", 7));
        verificarInvariantes(ordenId);
    }

    @Test
    @DisplayName("""
            Escenario: Despacho parcial recibido completo
              Dado que el despacho fue parcial y recibo todo lo despachado
              Cuando registro la recepción
              Entonces queda pendiente por despachar y la orden pasa a REABIERTA""")
    void despachoParcialRecibidoCompleto() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 6, 1, "ENTREGADO");

        recibir(parada, 6, receptor)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discrepancia").value(0))
                .andExpect(jsonPath("$.estadoOrden").value("REABIERTA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(4));

        assertThat(aLaManoBodega()).isEqualTo(6);
        assertThat(entranteBodega()).isEqualTo(4);
        verificarInvariantes(ordenId);
    }

    @Test
    @DisplayName("""
            Escenario: Nuevo ciclo tras reapertura
              Dado que una orden está REABIERTA
              Cuando se despacha el pendiente
              Entonces se crea un nuevo Ciclo_Despacho sobre la misma orden""")
    void nuevoCicloTrasReapertura() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada primera = despachar(ordenId, 10, 1, "ENTREGADO");
        recibir(primera, 7, receptor).andExpect(status().isCreated());
        assertThat(estadoOrden(ordenId)).isEqualTo("REABIERTA");

        Parada segunda = despachar(ordenId, 3, 2, "ENTREGADO");
        recibir(segunda, 3, receptor)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cicloDespachoId").value(segunda.cicloId().toString()))
                .andExpect(jsonPath("$.estadoOrden").value("CERRADA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(0));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ciclo_despacho WHERE orden_id = ?",
                Integer.class, ordenId)).isEqualTo(2);
        assertThat(aLaManoBodega()).isEqualTo(10);
        verificarInvariantes(ordenId);
    }

    @Test
    @DisplayName("""
            Escenario: Cantidad inválida
              Cuando la cantidad recibida es negativa
              Entonces el sistema responde 400
              Cuando es mayor que la despachada
              Entonces el sistema responde 422""")
    void cantidadInvalida() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");

        recibir(parada, -1, receptor).andExpect(status().isBadRequest());
        recibir(parada, 11, receptor)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensaje").exists());
        mockMvc.perform(post(ruta(parada)).header(HttpHeaders.AUTHORIZATION, receptor.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(recepcionesDe(parada)).isZero();
        assertThat(aLaManoBodega()).isZero();
        assertThat(estadoOrden(ordenId)).isEqualTo("EN_TRANSITO");
    }

    @Test
    @DisplayName("""
            Escenario: Reintento
              Dado que la parada ya tiene una recepción
              Cuando repito el registro
              Entonces el sistema responde 200 con la recepción original y no vuelve a sumar stock""")
    void reintento() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");
        String id = com.jayway.jsonpath.JsonPath.read(recibir(parada, 7, receptor)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        recibir(parada, 9, receptor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.cantidadRecibida").value(7))
                .andExpect(jsonPath("$.estadoOrden").value("REABIERTA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(3));

        assertThat(recepcionesDe(parada)).isEqualTo(1);
        assertThat(aLaManoBodega()).isEqualTo(7);
        verificarInvariantes(ordenId);
    }

    @Test
    @DisplayName("""
            Escenario: Variante nueva en la Bodega
              Dado que la variante nunca estuvo en la Bodega
              Cuando registro la recepción
              Entonces se crea la fila de inventario con lo recibido""")
    void varianteNuevaEnLaBodega() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");
        // La reserva ya creó la fila de la Bodega (#50); se quita para reproducir una variante que nunca estuvo.
        jdbc.update("DELETE FROM inventario WHERE nodo_id = ? AND variante_id = ?", bodegaId, varianteId);

        recibir(parada, 10, receptor).andExpect(status().isCreated());

        assertThat(aLaManoBodega()).isEqualTo(10);
    }

    @Test
    @DisplayName("""
            Escenario: Tienda fuera del alcance
              Dado que la tienda no está entre las del usuario
              Cuando registro la recepción
              Entonces el sistema responde 404""")
    void tiendaFueraDelAlcance() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");
        UsuarioDePrueba ajeno = usuarios.conScopes(Set.of(RECEPCION), crearTienda());

        recibir(parada, 10, ajeno).andExpect(status().isNotFound());

        assertThat(recepcionesDe(parada)).isZero();
        assertThat(aLaManoBodega()).isZero();
    }

    @Test
    @DisplayName("La parada debe estar ENTREGADO (409), ser del viaje indicado (404) y exige el scope (403)")
    void estadoViajeYScope() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada enTransito = despachar(ordenId, 10, 1, "EN_TRANSITO");

        recibir(enTransito, 10, receptor).andExpect(status().isConflict());
        recibir(new Parada(UUID.randomUUID(), enTransito.id(), enTransito.cicloId()), 10, receptor)
                .andExpect(status().isNotFound());
        UsuarioDePrueba sinScope = usuarios.conScopes(Set.of("inventario:consultar"), tiendaId);
        recibir(enTransito, 10, sinScope).andExpect(status().isForbidden());

        assertThat(recepcionesDe(enTransito)).isZero();
    }

    @Test
    @DisplayName("La orden debe estar EN_TRANSITO: si no, 409 y no se suma stock ni queda recepción")
    void ordenQueNoEstaEnTransito() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");
        jdbc.update("UPDATE orden_reabastecimiento SET estado = 'DESPACHADA' WHERE id = ?", ordenId);

        recibir(parada, 10, receptor).andExpect(status().isConflict());

        assertThat(recepcionesDe(parada)).isZero();
        assertThat(aLaManoBodega()).isZero();
    }

    @Test
    @DisplayName("Recibir 0 guarda la discrepancia completa, no toca el inventario y reabre la orden")
    void recibirCero() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");

        recibir(parada, 0, receptor)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discrepancia").value(10))
                .andExpect(jsonPath("$.estadoOrden").value("REABIERTA"))
                .andExpect(jsonPath("$.cantidadPendienteOrden").value(10));

        assertThat(aLaManoBodega()).isZero();
        assertThat(kardexBodega()).isEmpty();
        verificarInvariantes(ordenId);
    }

    @Test
    @DisplayName("GET /paradas-por-recibir lista las paradas ENTREGADO sin recepción de las tiendas del usuario")
    void paradasPorRecibir() throws Exception {
        UUID ordenA = crearOrden(5);
        Parada entregada = despachar(ordenA, 5, 1, "ENTREGADO");
        UUID ordenB = crearOrden(5);
        despachar(ordenB, 5, 1, "EN_TRANSITO");

        mockMvc.perform(get("/api/v1/paradas-por-recibir").header(HttpHeaders.AUTHORIZATION, receptor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paradaId").value(entregada.id().toString()))
                .andExpect(jsonPath("$[0].viajeId").value(entregada.viajeId().toString()))
                .andExpect(jsonPath("$[0].ordenId").value(ordenA.toString()))
                .andExpect(jsonPath("$[0].cantidadDespachada").value(5));

        UsuarioDePrueba ajeno = usuarios.conScopes(Set.of(RECEPCION), crearTienda());
        mockMvc.perform(get("/api/v1/paradas-por-recibir").header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        UsuarioDePrueba global = usuarios.conScopes(Set.of(RECEPCION, UsuarioActual.SCOPE_OPERAR_TODAS_LAS_TIENDAS));
        mockMvc.perform(get("/api/v1/paradas-por-recibir").header(HttpHeaders.AUTHORIZATION, global.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.paradaId == '" + entregada.id() + "')]").exists());

        recibir(entregada, 5, receptor).andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/paradas-por-recibir").header(HttpHeaders.AUTHORIZATION, receptor.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Recepciones simultáneas de la misma parada: una sola suma stock y las demás devuelven la original")
    void recepcionesConcurrentes() throws Exception {
        UUID ordenId = crearOrden(10);
        Parada parada = despachar(ordenId, 10, 1, "ENTREGADO");
        UsuarioActual actor = new UsuarioActual(receptor.id(), Set.of(RECEPCION), Set.of(tiendaId));
        int hilos = 4;

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<ResultadoRecepcion>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return registrarRecepcion.registrar(new Comando(actor, parada.viajeId(), parada.id(), 8));
            }));
        }
        salida.countDown();
        List<ResultadoRecepcion> resultados = new ArrayList<>();
        for (Future<ResultadoRecepcion> futuro : futuros) {
            resultados.add(futuro.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(resultados).filteredOn(ResultadoRecepcion::creada).hasSize(1);
        assertThat(resultados).extracting(r -> r.recepcion().getId()).containsOnly(resultados.getFirst().recepcion().getId());
        assertThat(recepcionesDe(parada)).isEqualTo(1);
        assertThat(aLaManoBodega()).isEqualTo(8);
        verificarInvariantes(ordenId);
    }

    // -------------------------------------------------------------------------
    // Invariantes de 2.5: P = S − ΣR; entrante = S − ΣR (nada liberado); ΣD = ΣR + Σdiscrepancia
    // -------------------------------------------------------------------------

    private void verificarInvariantes(UUID ordenId) {
        var orden = jdbc.queryForMap("""
                SELECT cantidad_total_solicitada, cantidad_pendiente FROM orden_reabastecimiento WHERE id = ?""",
                ordenId);
        int s = (Integer) orden.get("cantidad_total_solicitada");
        var sumas = jdbc.queryForMap("""
                SELECT COALESCE(SUM(r.cantidad_recibida), 0) AS recibido,
                       COALESCE(SUM(r.discrepancia), 0) AS discrepancia
                FROM recepcion r JOIN ciclo_despacho c ON c.id = r.ciclo_despacho_id
                WHERE c.orden_id = ?""", ordenId);
        int recibido = ((Number) sumas.get("recibido")).intValue();
        int discrepancia = ((Number) sumas.get("discrepancia")).intValue();
        int despachadoRecibido = jdbc.queryForObject("""
                SELECT COALESCE(SUM(c.cantidad_despachada), 0) FROM ciclo_despacho c
                WHERE c.orden_id = ? AND EXISTS (SELECT 1 FROM recepcion r WHERE r.ciclo_despacho_id = c.id)""",
                Integer.class, ordenId);

        assertThat(orden.get("cantidad_pendiente")).isEqualTo(s - recibido);
        assertThat(entranteBodega()).isEqualTo(s - recibido);
        assertThat(despachadoRecibido).isEqualTo(recibido + discrepancia);
    }

    // -------------------------------------------------------------------------

    private record Parada(UUID viajeId, UUID id, UUID cicloId) {
    }

    private ResultActions recibir(Parada parada, int cantidad, UsuarioDePrueba usuario) throws Exception {
        return mockMvc.perform(post(ruta(parada))
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidadRecibida\": " + cantidad + "}"));
    }

    private static String ruta(Parada parada) {
        return "/api/v1/viajes/" + parada.viajeId() + "/paradas/" + parada.id() + "/recepcion";
    }

    private UUID crearOrden(int cantidad) {
        return tx.execute(estado -> crearOrden.crear(null, tiendaId, varianteId, cantidad)).getId();
    }

    /**
     * Despacha un ciclo de la orden con los contratos reales (HU-46 hará esto) y crea su viaje y su
     * parada con SQL sobre las tablas de V1 (HU-47 y HU-48 harán esto).
     */
    private Parada despachar(UUID ordenId, int cantidad, int numeroCiclo, String estadoParada) {
        UUID cicloId = UUID.randomUUID();
        tx.executeWithoutResult(estado -> {
            OrdenParaDespacho orden = transiciones.iniciarDespacho(ordenId);
            jdbc.update("""
                    INSERT INTO ciclo_despacho (id, orden_id, numero_ciclo, cantidad_solicitada, cantidad_despachada,
                                                resultado)
                    VALUES (?, ?, ?, ?, ?, ?)""", cicloId, ordenId, numeroCiclo, orden.cantidadSolicitadaCiclo(), cantidad,
                    cantidad < orden.cantidadSolicitadaCiclo() ? "PARCIAL" : "COMPLETO");
            movimientos.registrarDespacho(orden.cdId(), orden.varianteId(), cantidad,
                    Math.min(cantidad, orden.reservaRestante()), cicloId);
            transiciones.confirmarDespacho(ordenId, cantidad);
            transiciones.marcarEnTransito(ordenId);
        });
        UUID viajeId = UUID.randomUUID();
        jdbc.update("INSERT INTO viaje (id, cd_id, vehiculo) VALUES (?, ?, 'Camión 1')", viajeId, cdId);
        UUID paradaId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO viaje_parada (id, viaje_id, ciclo_despacho_id, tienda_id, orden_parada, estado)
                VALUES (?, ?, ?, ?, 1, ?)""", paradaId, viajeId, cicloId, tiendaId, estadoParada);
        return new Parada(viajeId, paradaId, cicloId);
    }

    private int recepcionesDe(Parada parada) {
        return jdbc.queryForObject("SELECT count(*) FROM recepcion WHERE viaje_parada_id = ?",
                Integer.class, parada.id());
    }

    private String estadoOrden(UUID ordenId) {
        return jdbc.queryForObject("SELECT estado FROM orden_reabastecimiento WHERE id = ?", String.class, ordenId);
    }

    private int aLaManoBodega() {
        return jdbc.query("SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                (rs, n) -> rs.getInt(1), bodegaId, varianteId).stream().findFirst().orElse(0);
    }

    private int entranteBodega() {
        return jdbc.query("SELECT entrante FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                (rs, n) -> rs.getInt(1), bodegaId, varianteId).stream().findFirst().orElse(0);
    }

    private List<Map<String, Object>> kardexBodega() {
        return jdbc.query("""
                SELECT tipo, cantidad FROM movimiento_inventario
                WHERE nodo_id = ? AND variante_id = ?""",
                (rs, n) -> Map.<String, Object>of("tipo", rs.getString("tipo"), "cantidad", rs.getInt("cantidad")),
                bodegaId, varianteId);
    }

    private UUID cdConStock(int cantidad) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", id, "CD " + id);
        UUID nodo = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodo, id);
        inventario.cargar(nodo, varianteId, cantidad, null);
        return id;
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", id, "Tienda " + id);
        return id;
    }

    private UUID nodoDeTienda(UUID tienda) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", id, tienda);
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
