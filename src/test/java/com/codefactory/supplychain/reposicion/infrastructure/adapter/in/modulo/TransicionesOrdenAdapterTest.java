package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.modulo;

import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.reposicion.api.ConsultaOrdenes;
import com.codefactory.supplychain.reposicion.api.OrdenEstadoInvalidoException;
import com.codefactory.supplychain.reposicion.api.OrdenNoEncontradaException;
import com.codefactory.supplychain.reposicion.api.OrdenParaDespacho;
import com.codefactory.supplychain.reposicion.api.ResultadoRecepcion;
import com.codefactory.supplychain.reposicion.api.TransicionesOrden;
import com.codefactory.supplychain.reposicion.application.port.in.CrearOrdenUseCase;
import com.codefactory.supplychain.reposicion.domain.model.OrdenReabastecimiento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Transiciones de {@code reposicion/api} contra PostgreSQL real,
 * Se comprueban los invariantes que viven en la orden tras cada paso. 
 * Logística no existe todavía: los ΣD y ΣR se llevan en la prueba
 * y no se mueve inventario (eso lo hará logística con inventario/api).
 *
 * Módulo: reposicion.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DisplayName("TransicionesOrden (reposicion/api)")
class TransicionesOrdenAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private TransicionesOrden transiciones;
    @Autowired private ConsultaOrdenes consulta;
    @Autowired private CrearOrdenUseCase crearOrden;
    @Autowired private InventarioUseCase inventario;
    @Autowired private TransactionTemplate tx;
    @Autowired private JdbcTemplate jdbc;

    private UUID tiendaId;
    private UUID varianteId;
    private UUID cdId;

    @BeforeEach
    void setUp() {
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')", tiendaId, "Tienda " + tiendaId);
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", UUID.randomUUID(), tiendaId);
        varianteId = crearVariante();
        cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        UUID nodoCd = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodoCd, cdId);
        inventario.cargar(nodoCd, varianteId, 100, null);
        jdbc.update("INSERT INTO tienda_cd_prioridad (tienda_id, cd_id, orden_prioridad) VALUES (?, ?, 1)",
                tiendaId, cdId);
    }

    @Test
    @DisplayName("""
            Escenario: ciclo completo con recepción total
              Dado una orden APROBADA de 10 unidades
              Cuando se inicia el despacho, se despachan 10, sale el viaje y se reciben 10
              Entonces pasa por EN_DESPACHO, DESPACHADA y EN_TRANSITO y termina CERRADA con pendiente 0
              Y tras cada paso se cumplen los invariantes de 2.5""")
    void cicloCompletoTotal() {
        UUID ordenId = crearAprobada(10);
        verificar(ordenId, "APROBADA", 10, 0, 0);

        OrdenParaDespacho datos = tx.execute(s -> transiciones.iniciarDespacho(ordenId));
        assertThat(datos).isEqualTo(new OrdenParaDespacho(cdId, tiendaId, varianteId, 10, 10));
        verificar(ordenId, "EN_DESPACHO", 10, 0, 0);

        tx.executeWithoutResult(s -> transiciones.confirmarDespacho(ordenId, 10));
        verificar(ordenId, "DESPACHADA", 10, 10, 0);

        tx.executeWithoutResult(s -> transiciones.marcarEnTransito(ordenId));
        verificar(ordenId, "EN_TRANSITO", 10, 10, 0);

        ResultadoRecepcion resultado = tx.execute(s -> transiciones.registrarRecepcion(ordenId, 10));
        assertThat(resultado).isEqualTo(new ResultadoRecepcion("CERRADA", 0));
        verificar(ordenId, "CERRADA", 10, 10, 10);
    }

    @Test
    @DisplayName("""
            Escenario: recepción parcial que reabre y nuevo ciclo
              Dado una orden APROBADA de 10 unidades despachada completa
              Cuando llegan solo 7 (3 perdidas en tránsito)
              Entonces queda REABIERTA con pendiente 3 y reserva restante 0
              Y un nuevo ciclo solicita 3, se despachan y reciben 3 y la orden queda CERRADA""")
    void parcialQueReabre() {
        UUID ordenId = crearAprobada(10);
        tx.execute(s -> transiciones.iniciarDespacho(ordenId));
        tx.executeWithoutResult(s -> transiciones.confirmarDespacho(ordenId, 10));
        tx.executeWithoutResult(s -> transiciones.marcarEnTransito(ordenId));

        ResultadoRecepcion parcial = tx.execute(s -> transiciones.registrarRecepcion(ordenId, 7));
        assertThat(parcial).isEqualTo(new ResultadoRecepcion("REABIERTA", 3));
        verificar(ordenId, "REABIERTA", 10, 10, 7);
        // Pérdida en tránsito ΣD − ΣR = 3 (la discrepancia la registra logística).

        OrdenParaDespacho segundoCiclo = tx.execute(s -> transiciones.iniciarDespacho(ordenId));
        // Reserva restante max(S − ΣD, 0) = 0: la reserva ya se consumió en el primer despacho.
        assertThat(segundoCiclo.cantidadSolicitadaCiclo()).isEqualTo(3);
        assertThat(segundoCiclo.reservaRestante()).isZero();

        tx.executeWithoutResult(s -> transiciones.confirmarDespacho(ordenId, 3));
        verificar(ordenId, "DESPACHADA", 10, 13, 7);
        tx.executeWithoutResult(s -> transiciones.marcarEnTransito(ordenId));
        ResultadoRecepcion cierre = tx.execute(s -> transiciones.registrarRecepcion(ordenId, 3));
        assertThat(cierre).isEqualTo(new ResultadoRecepcion("CERRADA", 0));
        verificar(ordenId, "CERRADA", 10, 13, 10);
    }

    @Test
    @DisplayName("""
            Escenario: despacho parcial recibido completo
              Dado una orden de 10 de la que solo se despachan 6
              Cuando se reciben los 6
              Entonces queda REABIERTA con pendiente 4 y reserva restante 4""")
    void despachoParcialRecibidoCompleto() {
        UUID ordenId = crearAprobada(10);
        tx.execute(s -> transiciones.iniciarDespacho(ordenId));
        tx.executeWithoutResult(s -> transiciones.confirmarDespacho(ordenId, 6));
        verificar(ordenId, "DESPACHADA", 10, 6, 0);
        tx.executeWithoutResult(s -> transiciones.marcarEnTransito(ordenId));

        ResultadoRecepcion recepcion = tx.execute(s -> transiciones.registrarRecepcion(ordenId, 6));
        assertThat(recepcion).isEqualTo(new ResultadoRecepcion("REABIERTA", 4));
        verificar(ordenId, "REABIERTA", 10, 6, 6);
        OrdenParaDespacho nuevoCiclo = tx.execute(s -> transiciones.iniciarDespacho(ordenId));
        assertThat(nuevoCiclo).isEqualTo(new OrdenParaDespacho(cdId, tiendaId, varianteId, 4, 4));
    }

    @Test
    @DisplayName("""
            Escenario: estado inválido
              Dado una orden GENERADA sin CD
              Cuando logística intenta iniciar el despacho, confirmarlo, marcarla en tránsito o recibirla
              Entonces recibe OrdenEstadoInvalidoException y la orden no cambia""")
    void estadoInvalido() {
        UUID sinCd = tx.execute(s -> crearOrden.crear(null, tiendaSinCd(), varianteId, 5)).getId();

        assertThatThrownBy(() -> tx.execute(s -> transiciones.iniciarDespacho(sinCd)))
                .isInstanceOf(OrdenEstadoInvalidoException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> transiciones.confirmarDespacho(sinCd, 5)))
                .isInstanceOf(OrdenEstadoInvalidoException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> transiciones.marcarEnTransito(sinCd)))
                .isInstanceOf(OrdenEstadoInvalidoException.class);
        assertThatThrownBy(() -> tx.execute(s -> transiciones.registrarRecepcion(sinCd, 5)))
                .isInstanceOf(OrdenEstadoInvalidoException.class);
        assertThat(consulta.consultarOrden(sinCd).estado()).isEqualTo("GENERADA");
    }

    @Test
    @DisplayName("orden inexistente: OrdenNoEncontradaException en todas las transiciones")
    void ordenInexistente() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> transiciones.iniciarDespacho(id)).isInstanceOf(OrdenNoEncontradaException.class);
        assertThatThrownBy(() -> transiciones.confirmarDespacho(id, 1)).isInstanceOf(OrdenNoEncontradaException.class);
        assertThatThrownBy(() -> transiciones.marcarEnTransito(id)).isInstanceOf(OrdenNoEncontradaException.class);
        assertThatThrownBy(() -> transiciones.registrarRecepcion(id, 1)).isInstanceOf(OrdenNoEncontradaException.class);
    }

    // -------------------------------------------------------------------------

    /**
     * Invariantes que viven en la orden:
     * P = S − ΣR; ΣD acumulado; ΣR ≤ ΣD; P = 0 ⇔ CERRADA. La reserva restante
     * max(S − ΣD, 0) se comprueba en lo que devuelve {@code iniciarDespacho}.
     */
    private void verificar(UUID ordenId, String estado, int s, int sumaD, int sumaR) {
        Map<String, Object> fila = jdbc.queryForMap("""
                SELECT estado, cantidad_total_solicitada, cantidad_pendiente, cantidad_despachada_acumulada
                FROM orden_reabastecimiento WHERE id = ?""", ordenId);
        assertThat(fila.get("estado")).isEqualTo(estado);
        assertThat(fila.get("cantidad_total_solicitada")).isEqualTo(s);
        assertThat(fila.get("cantidad_despachada_acumulada")).isEqualTo(sumaD);
        assertThat(fila.get("cantidad_pendiente")).isEqualTo(s - sumaR);
        assertThat(sumaR).isLessThanOrEqualTo(sumaD);
        assertThat(consulta.consultarOrden(ordenId).cantidadPendiente()).isEqualTo(s - sumaR);
        assertThat((Integer) fila.get("cantidad_pendiente") == 0).isEqualTo("CERRADA".equals(estado));
    }

    private UUID crearAprobada(int cantidad) {
        OrdenReabastecimiento orden = tx.execute(s -> crearOrden.crear(null, tiendaId, varianteId, cantidad));
        assertThat(orden.getCdAsignadoId()).isEqualTo(cdId);
        return orden.getId();
    }

    private UUID tiendaSinCd() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Cali')", id, "Tienda " + id);
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", UUID.randomUUID(), id);
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
