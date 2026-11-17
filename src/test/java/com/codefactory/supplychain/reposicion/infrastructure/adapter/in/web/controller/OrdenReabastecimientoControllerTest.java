package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.web.controller;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Criterios de extremo a extremo contra PostgreSQL real: fallback de CD,
 * concurrencia sobre el último stock, invariantes tras crear la orden y la
 * consulta REST.
 * <p>
 * La creación ocurre al aprobar: aquí se llama
 * {@link CrearOrdenUseCase} dentro de una transacción, como lo hará la aprobación, y
 * el reintento por {@code @Version} se hace fuera de ella, como lo hará su caso de uso
 * de entrada.
 *
 * Módulo: reposicion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
@DisplayName("Orden de reabastecimiento")
class OrdenReabastecimientoControllerTest {

    private static final String PLANIFICAR = "reposicion:planificar";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuariosDePrueba usuarios;
    @Autowired private InventarioUseCase inventario;
    @Autowired private CrearOrdenUseCase crearOrden;
    @Autowired private TransactionTemplate tx;

    private UUID tiendaId;
    private UUID bodegaId;
    private UUID varianteId;
    private UsuarioDePrueba planificador;

    @BeforeEach
    void setUp() {
        jdbc.update("""
                INSERT INTO scope (codigo, descripcion, sensible) VALUES (?, 'Planificar reposición al CD', false)
                ON CONFLICT (codigo) DO NOTHING""", PLANIFICAR);
        tiendaId = crearTienda();
        bodegaId = nodoDeTienda(tiendaId, "BODEGA_TIENDA");
        varianteId = crearVariante();
        planificador = usuarios.conScopes(Set.of(PLANIFICAR), tiendaId);
    }

    @Test
    @DisplayName("""
            Escenario: Primer CD con disponibilidad
              Dado que el CD de mayor prioridad tiene disponible suficiente
              Cuando se crea la Orden de Reabastecimiento
              Entonces se asigna ese CD y la orden queda APROBADA
              Y la cantidad queda reservada en el CD y sumada como entrante en la Bodega""")
    void primerCdConDisponibilidad() throws Exception {
        UUID cd1 = cdConStock(30);
        UUID cd2 = cdConStock(30);
        priorizar(tiendaId, cd1, 1);
        priorizar(tiendaId, cd2, 2);

        OrdenReabastecimiento orden = crear(tiendaId, 10);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.APROBADA);
        assertThat(orden.getCdAsignadoId()).isEqualTo(cd1);
        assertThat(saliente(cd1)).isEqualTo(10);
        assertThat(saliente(cd2)).isZero();
        assertThat(entranteBodega(tiendaId)).isEqualTo(10);
        verificarInvariantesTrasCrear(orden);

        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento/{id}", orden.getId())
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.cdAsignadoId").value(cd1.toString()))
                .andExpect(jsonPath("$.intentos.length()").value(1))
                .andExpect(jsonPath("$.intentos[0].resultado").value("ASIGNADO"));
    }

    @Test
    @DisplayName("""
            Escenario: Fallback al siguiente CD
              Dado que el primer CD no tiene disponibilidad suficiente
              Cuando se crea la orden
              Entonces se evalúa el siguiente CD de la lista
              Y se registra en Intento_CD cada CD evaluado, el resultado y el motivo de descarte""")
    void fallbackAlSiguienteCd() throws Exception {
        UUID cd1 = cdConStock(4); // cubre solo una parte: no se divide la orden (D-83)
        UUID cd2 = cdConStock(30);
        priorizar(tiendaId, cd1, 1);
        priorizar(tiendaId, cd2, 2);

        OrdenReabastecimiento orden = crear(tiendaId, 10);

        assertThat(orden.getCdAsignadoId()).isEqualTo(cd2);
        assertThat(saliente(cd1)).isZero();
        assertThat(saliente(cd2)).isEqualTo(10);
        verificarInvariantesTrasCrear(orden);

        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento/{id}", orden.getId())
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intentos.length()").value(2))
                .andExpect(jsonPath("$.intentos[0].cdId").value(cd1.toString()))
                .andExpect(jsonPath("$.intentos[0].ordenEvaluado").value(1))
                .andExpect(jsonPath("$.intentos[0].resultado").value("SIN_STOCK"))
                .andExpect(jsonPath("$.intentos[0].motivo").value("Disponible 4 menor que lo requerido 10"))
                .andExpect(jsonPath("$.intentos[1].cdId").value(cd2.toString()))
                .andExpect(jsonPath("$.intentos[1].resultado").value("ASIGNADO"));
    }

    @Test
    @DisplayName("""
            Escenario: Ningún CD disponible
              Dado que ningún CD de la lista tiene disponibilidad
              Cuando se crea la orden
              Entonces la orden queda GENERADA sin CD asignado y con todos los intentos
              Y no se reserva ni se suma entrante""")
    void ningunCdDisponible() throws Exception {
        UUID cd1 = cdConStock(2);
        UUID cd2 = cdConStock(5);
        priorizar(tiendaId, cd1, 1);
        priorizar(tiendaId, cd2, 2);

        OrdenReabastecimiento orden = crear(tiendaId, 10);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.GENERADA);
        assertThat(orden.getCdAsignadoId()).isNull();
        assertThat(saliente(cd1)).isZero();
        assertThat(saliente(cd2)).isZero();
        assertThat(entranteBodega(tiendaId)).isZero();
        verificarInvariantesTrasCrear(orden);

        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento/{id}", orden.getId())
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("GENERADA"))
                .andExpect(jsonPath("$.cdAsignadoId").value(nullValue()))
                .andExpect(jsonPath("$.intentos.length()").value(2))
                .andExpect(jsonPath("$.intentos[1].resultado").value("SIN_STOCK"));
    }

    @Test
    @DisplayName("""
            Escenario: Concurrencia sobre el último stock
              Dado que dos órdenes compiten por el último stock de un CD
              Cuando se crean al mismo tiempo
              Entonces una queda asignada a ese CD y la otra pasa al siguiente CD de su lista""")
    void concurrenciaSobreElUltimoStock() throws Exception {
        UUID cdCompartido = cdConStock(10); // alcanza para una sola orden
        UUID cdRespaldo = cdConStock(30);
        UUID otraTienda = crearTienda();
        nodoDeTienda(otraTienda, "BODEGA_TIENDA");
        for (UUID tienda : List.of(tiendaId, otraTienda)) {
            priorizar(tienda, cdCompartido, 1);
            priorizar(tienda, cdRespaldo, 2);
        }

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<OrdenReabastecimiento>> resultados = List.of(
                hilos.submit(crearConReintento(salida, tiendaId)),
                hilos.submit(crearConReintento(salida, otraTienda)));
        salida.countDown();
        OrdenReabastecimiento a = resultados.get(0).get(60, TimeUnit.SECONDS);
        OrdenReabastecimiento b = resultados.get(1).get(60, TimeUnit.SECONDS);
        hilos.shutdown();

        assertThat(List.of(a.getCdAsignadoId(), b.getCdAsignadoId()))
                .containsExactlyInAnyOrder(cdCompartido, cdRespaldo);
        assertThat(saliente(cdCompartido)).isEqualTo(10);
        assertThat(disponible(cdCompartido)).isZero();
        assertThat(saliente(cdRespaldo)).isEqualTo(10);
        OrdenReabastecimiento perdedora = a.getCdAsignadoId().equals(cdRespaldo) ? a : b;
        assertThat(perdedora.getIntentos()).hasSize(2);
        verificarInvariantesTrasCrear(a);
        verificarInvariantesTrasCrear(b);
    }

    @Test
    @DisplayName("""
            Escenario: Tienda sin CDs en su prioridad
              Dado que la tienda no tiene CDs configurados
              Cuando se crea la orden
              Entonces queda GENERADA sin CD y sin intentos""")
    void tiendaSinCds() {
        OrdenReabastecimiento orden = crear(tiendaId, 10);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrden.GENERADA);
        assertThat(orden.getCdAsignadoId()).isNull();
        assertThat(intentosEnBd(orden.getId())).isZero();
        verificarInvariantesTrasCrear(orden);
    }

    @Test
    @DisplayName("GET /ordenes-reabastecimiento filtra por estado y tienda; fuera de alcance responde 404")
    void listarYAlcance() throws Exception {
        UUID cd = cdConStock(30);
        priorizar(tiendaId, cd, 1);
        OrdenReabastecimiento aprobada = crear(tiendaId, 5);
        crear(tiendaId, 50); // no cubre: GENERADA

        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento")
                        .param("tiendaId", tiendaId.toString())
                        .param("estado", "APROBADA")
                        .header(HttpHeaders.AUTHORIZATION, planificador.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aprobada.getId().toString()));

        UsuarioDePrueba ajeno = usuarios.conScopes(Set.of(PLANIFICAR));
        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento/{id}", aprobada.getId())
                        .header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento").param("tiendaId", tiendaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, ajeno.bearer()))
                .andExpect(status().isNotFound());

        UsuarioDePrueba sinScope = usuarios.conScopes(Set.of("reposicion:administrar"), tiendaId);
        mockMvc.perform(get("/api/v1/ordenes-reabastecimiento")
                        .header(HttpHeaders.AUTHORIZATION, sinScope.bearer()))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Invariantes de 2.5 recién creada la orden (ΣD = 0, ΣR = 0, nada liberado)
    // -------------------------------------------------------------------------

    private void verificarInvariantesTrasCrear(OrdenReabastecimiento orden) {
        var fila = jdbc.queryForMap("""
                SELECT cantidad_total_solicitada, cantidad_pendiente, cantidad_despachada_acumulada,
                       cd_asignado_id, estado
                FROM orden_reabastecimiento WHERE id = ?""", orden.getId());
        int s = (Integer) fila.get("cantidad_total_solicitada");
        // Pendiente P = S − ΣR = S.
        assertThat(fila.get("cantidad_pendiente")).isEqualTo(s);
        assertThat(fila.get("cantidad_despachada_acumulada")).isEqualTo(0);
        // Reserva viva = max(S − ΣD, 0) = S.
        assertThat(orden.reservaRestante()).isEqualTo(s);
        if (fila.get("cd_asignado_id") != null) {
            assertThat(fila.get("estado")).isEqualTo("APROBADA");
            // entrante de la Bodega incluye S − ΣR − liberado = S.
            assertThat(entranteBodega(orden.getTiendaId())).isGreaterThanOrEqualTo(s);
        } else {
            // Sin CD no se toca el inventario (D-102).
            assertThat(fila.get("estado")).isEqualTo("GENERADA");
        }
    }

    // -------------------------------------------------------------------------

    private OrdenReabastecimiento crear(UUID tienda, int cantidad) {
        return tx.execute(estado -> crearOrden.crear(null, tienda, varianteId, cantidad));
    }

    /** Reintento fuera de la transacción, como hará el caso de uso de entrada (D-24). */
    private Callable<OrdenReabastecimiento> crearConReintento(CountDownLatch salida, UUID tienda) {
        return () -> {
            salida.await();
            for (int intento = 1; ; intento++) {
                try {
                    return crear(tienda, 10);
                } catch (OptimisticLockingFailureException conflicto) {
                    if (intento == 4) {
                        throw conflicto;
                    }
                }
            }
        };
    }

    private UUID cdConStock(int cantidad) {
        UUID cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        UUID nodo = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodo, cdId);
        inventario.cargar(nodo, varianteId, cantidad, null);
        return cdId;
    }

    private void priorizar(UUID tienda, UUID cdId, int prioridad) {
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, ?)",
                tienda, cdId, prioridad);
    }

    private int saliente(UUID cdId) {
        return jdbc.queryForObject("""
                SELECT i.saliente FROM inventario i JOIN nodo n ON n.id = i.nodo_id
                WHERE n.cd_id = ? AND i.variante_id = ?""", Integer.class, cdId, varianteId);
    }

    private int disponible(UUID cdId) {
        return jdbc.queryForObject("""
                SELECT i.disponible_para_uso FROM inventario i JOIN nodo n ON n.id = i.nodo_id
                WHERE n.cd_id = ? AND i.variante_id = ?""", Integer.class, cdId, varianteId);
    }

    private int entranteBodega(UUID tienda) {
        return jdbc.query("""
                SELECT i.entrante FROM inventario i JOIN nodo n ON n.id = i.nodo_id
                WHERE n.tienda_id = ? AND n.tipo = 'BODEGA_TIENDA' AND i.variante_id = ?""",
                (rs, n) -> rs.getInt(1), tienda, varianteId).stream().findFirst().orElse(0);
    }

    private int intentosEnBd(UUID ordenId) {
        return jdbc.queryForObject("SELECT count(*) FROM intento_cd WHERE orden_id = ?", Integer.class, ordenId);
    }

    private UUID crearTienda() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", id, "Tienda " + id);
        return id;
    }

    private UUID nodoDeTienda(UUID tienda, String tipo) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, ?, ?)", id, tipo, tienda);
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
