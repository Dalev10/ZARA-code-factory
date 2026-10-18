package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.application.port.out.InventarioRepositoryPort;
import com.codefactory.supplychain.inventario.application.port.out.MovimientoInventarioRepositoryPort;
import com.codefactory.supplychain.inventario.domain.exception.StockInsuficienteException;
import com.codefactory.supplychain.inventario.domain.model.MovimientoInventario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Pruebas de integración de HU-27 contra PostgreSQL real: concurrencia con
 * bloqueo optimista (D-24), atomicidad inventario + kardex, publicación de
 * ExistenciasActualizadas y reintento optimista real.
 *
 * Todas las operaciones entran por {@link InventarioUseCase}, el punto de
 * entrada con reintento, nunca por el servicio transaccional interno.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class InventarioIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InventarioUseCase inventarioUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RegistroEventos registroEventos;

    @MockitoSpyBean
    private InventarioRepositoryPort inventarioRepositoryPort;

    @MockitoSpyBean
    private MovimientoInventarioRepositoryPort movimientoRepositoryPort;

    private UUID tiendaId;
    private UUID nodoId;
    private UUID varianteId;

    @BeforeEach
    void setUp() {
        tiendaId = UUID.randomUUID();
        nodoId = crearAlmacen();
        varianteId = crearVariante();
        registroEventos.limpiar();
    }

    @Test
    @DisplayName("""
            Escenario: venta concurrente no pierde descuentos
              Dado un nodo con variante cargada con 10 unidades
              Cuando 20 ventas de 1 unidad llegan simultáneamente
              Entonces el a_la_mano final es 10 menos las ventas exitosas y nunca negativo
              Y el kardex tiene exactamente un movimiento VENTA por venta exitosa
              Y las ventas fallidas solo fallan por stock insuficiente o por agotar los reintentos""")
    void ventaConcurrenteNoPierdeDescuentos() throws Exception {
        inventarioUseCase.cargar(nodoId, varianteId, 10);
        int ventas = 20;
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(ventas);
        List<Future<?>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < ventas; i++) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return inventarioUseCase.aplicarVenta(nodoId, varianteId, 1, false);
                }));
            }
            salida.countDown();

            int exitosas = 0;
            int sinStock = 0;
            int reintentosAgotados = 0;
            for (Future<?> resultado : resultados) {
                try {
                    resultado.get(60, TimeUnit.SECONDS);
                    exitosas++;
                } catch (java.util.concurrent.ExecutionException e) {
                    if (e.getCause() instanceof StockInsuficienteException) {
                        sinStock++;
                    } else if (e.getCause() instanceof OptimisticLockingFailureException) {
                        reintentosAgotados++;
                    } else {
                        throw e;
                    }
                }
            }

            assertThat(exitosas + sinStock + reintentosAgotados).isEqualTo(ventas);
            assertThat(exitosas).isBetween(1, 10);
            if (sinStock > 0) {
                // Solo se rechaza por stock cuando ya se vendieron las 10 unidades.
                assertThat(exitosas).isEqualTo(10);
            }
            assertThat(aLaMano()).isEqualTo(10 - exitosas).isNotNegative();
            assertThat(contarMovimientos("VENTA")).isEqualTo(exitosas);
            assertThat(registroEventos.confirmados()).hasSize(1 + exitosas);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("""
            Escenario: inventario y kardex se guardan atómicamente
              Dado un nodo con variante cargada con 5 unidades
              Cuando falla el registro del movimiento en el kardex durante una venta
              Entonces el a_la_mano sigue en 5
              Y no hay movimiento VENTA ni evento confirmado""")
    void inventarioYKardexSeGuardanAtomicamente() {
        inventarioUseCase.cargar(nodoId, varianteId, 5);
        registroEventos.limpiar();
        doThrow(new IllegalStateException("Fallo simulado del kardex"))
                .when(movimientoRepositoryPort).guardar(any(), any(), any());

        assertThatThrownBy(() -> inventarioUseCase.aplicarVenta(nodoId, varianteId, 2, false))
                .isInstanceOf(IllegalStateException.class);

        assertThat(aLaMano()).isEqualTo(5);
        assertThat(contarMovimientos("VENTA")).isZero();
        assertThat(registroEventos.confirmados()).isEmpty();
    }

    @Test
    @DisplayName("""
            Escenario: cada movimiento publica ExistenciasActualizadas dentro de la transacción
              Dado un nodo con variante cargada con 4 unidades
              Cuando se registra una venta forzada de 6 unidades
              Entonces se publica un evento dentro de una transacción activa
              Y tras el commit el evento coincide con el movimiento y el saldo resultante""")
    void cadaMovimientoPublicaElEventoConElSaldoResultante() {
        inventarioUseCase.cargar(nodoId, varianteId, 4);
        registroEventos.limpiar();

        MovimientoInventario movimiento = inventarioUseCase.aplicarVenta(nodoId, varianteId, 6, true);

        assertThat(registroEventos.publicadosEnTransaccion()).containsExactly(true);
        assertThat(registroEventos.confirmados()).singleElement().satisfies(evento -> {
            assertThat(evento.nodoId()).isEqualTo(nodoId);
            assertThat(evento.varianteId()).isEqualTo(varianteId);
            assertThat(evento.tipoNodo()).isEqualTo(TipoNodoInventario.ALMACEN);
            assertThat(evento.tiendaId()).isEqualTo(tiendaId);
            assertThat(evento.movimientoId()).isEqualTo(movimiento.id());
            assertThat(evento.tipoMovimiento()).isEqualTo(TipoMovimientoKardex.VENTA);
            assertThat(evento.forzado()).isTrue();
            assertThat(evento.aLaManoAnterior()).isEqualTo(4);
            assertThat(evento.aLaMano()).isEqualTo(-2);
            assertThat(evento.disponibleParaUso()).isEqualTo(-2);
            assertThat(evento.pronosticado()).isEqualTo(-2);
        });
        assertThat(aLaMano()).isEqualTo(-2);
        assertThat(jdbc.queryForObject(
                "SELECT forzado FROM movimiento_inventario WHERE id = ?",
                Boolean.class, movimiento.id())).isTrue();
    }

    @Test
    @DisplayName("""
            Escenario: un rollback no deja publicación efectiva
              Dado un nodo con variante cargada con 4 unidades
              Cuando una venta se ejecuta dentro de una transacción que se revierte
              Entonces el evento se publicó dentro de la transacción
              Y ningún listener AFTER_COMMIT lo recibe
              Y el a_la_mano sigue en 4""")
    void rollbackNoDejaPublicacionEfectiva() {
        inventarioUseCase.cargar(nodoId, varianteId, 4);
        registroEventos.limpiar();

        transactionTemplate.executeWithoutResult(estado -> {
            inventarioUseCase.aplicarVenta(nodoId, varianteId, 1, false);
            estado.setRollbackOnly();
        });

        assertThat(registroEventos.publicadosEnTransaccion()).containsExactly(true);
        assertThat(registroEventos.confirmados()).isEmpty();
        assertThat(aLaMano()).isEqualTo(4);
        assertThat(contarMovimientos("VENTA")).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: un conflicto de versión se reintenta releyendo la fila
              Dado un nodo con variante cargada con 10 unidades
              Y otra transacción suma 5 unidades mientras una venta está en curso
              Cuando la venta de 1 unidad choca por versión
              Entonces se reintenta, vuelve a leer la fila y termina con éxito
              Y el a_la_mano final es 14 con un solo movimiento VENTA""")
    void conflictoDeVersionSeReintentaReleyendoLaFila() throws Exception {
        inventarioUseCase.cargar(nodoId, varianteId, 10);
        clearInvocations(inventarioRepositoryPort);
        AtomicBoolean primeraLectura = new AtomicBoolean(true);
        ExecutorService otraConexion = Executors.newSingleThreadExecutor();
        try {
            doAnswer(invocacion -> {
                Object leido = invocacion.callRealMethod();
                if (primeraLectura.getAndSet(false)) {
                    otraConexion.submit(() -> jdbc.update("""
                            UPDATE inventario
                               SET a_la_mano = a_la_mano + 5,
                                   disponible_para_uso = disponible_para_uso + 5,
                                   pronosticado = pronosticado + 5,
                                   version = version + 1
                             WHERE nodo_id = ? AND variante_id = ?""", nodoId, varianteId))
                            .get(10, TimeUnit.SECONDS);
                }
                return leido;
            }).when(inventarioRepositoryPort).buscarPorNodoYVariante(nodoId, varianteId);

            MovimientoInventario movimiento = inventarioUseCase.aplicarVenta(nodoId, varianteId, 1, false);

            assertThat(movimiento.saldoResultante()).isEqualTo(14);
            assertThat(aLaMano()).isEqualTo(14);
            assertThat(contarMovimientos("VENTA")).isEqualTo(1);
            verify(inventarioRepositoryPort, times(2)).buscarPorNodoYVariante(nodoId, varianteId);
        } finally {
            otraConexion.shutdownNow();
        }
    }

    @Test
    @DisplayName("""
            Escenario: un error de negocio no se reintenta
              Dado un nodo con variante cargada con 1 unidad
              Cuando se registra una venta normal de 2 unidades
              Entonces se rechaza por stock insuficiente sin reintentar""")
    void errorDeNegocioNoSeReintenta() {
        inventarioUseCase.cargar(nodoId, varianteId, 1);
        clearInvocations(inventarioRepositoryPort);

        assertThatThrownBy(() -> inventarioUseCase.aplicarVenta(nodoId, varianteId, 2, false))
                .isInstanceOf(StockInsuficienteException.class);

        verify(inventarioRepositoryPort, times(1)).buscarPorNodoYVariante(nodoId, varianteId);
        assertThat(aLaMano()).isEqualTo(1);
    }

    @Test
    @DisplayName("""
            Escenario: una carga inicial en 0 deja la variante cargada
              Dado un nodo sin fila para la variante X
              Cuando se carga X con 0 unidades
              Entonces existe la fila con a_la_mano 0
              Y el kardex tiene un movimiento CARGA_INICIAL con cantidad 0""")
    void cargaInicialEnCeroDejaLaVarianteCargada() {
        inventarioUseCase.cargar(nodoId, varianteId, 0);

        assertThat(aLaMano()).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT cantidad FROM movimiento_inventario WHERE nodo_id = ? AND variante_id = ? AND tipo = 'CARGA_INICIAL'",
                Integer.class, nodoId, varianteId)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: la base rechaza movimientos en 0 que no son carga inicial
              Cuando se intenta guardar un movimiento AJUSTE con cantidad 0
              Entonces la base rechaza la operación""")
    void laBaseRechazaMovimientosEnCeroQueNoSonCarga() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO movimiento_inventario (nodo_id, variante_id, tipo, cantidad)
                VALUES (?, ?, 'AJUSTE', 0)""", nodoId, varianteId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int aLaMano() {
        return jdbc.queryForObject(
                "SELECT a_la_mano FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, nodoId, varianteId);
    }

    private int contarMovimientos(String tipo) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM movimiento_inventario WHERE nodo_id = ? AND variante_id = ? AND tipo = ?",
                Integer.class, nodoId, varianteId, tipo);
    }

    private UUID crearAlmacen() {
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda Inventario " + tiendaId);
        UUID almacenId = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'ALMACEN', ?)", almacenId, tiendaId);
        return almacenId;
    }

    private UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)",
                categoriaId, "Categoría Inventario " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template Inventario " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)",
                id, templateId, "SKU-" + id);
        return id;
    }

    @TestConfiguration
    static class ConfiguracionEventos {

        @Bean
        RegistroEventos registroEventos() {
            return new RegistroEventos();
        }
    }

    /**
     * Captura ExistenciasActualizadas en el momento de publicarse y después del
     * commit, para distinguir publicación de entrega efectiva.
     */
    static class RegistroEventos {

        private final List<Boolean> publicadosEnTransaccion = new CopyOnWriteArrayList<>();
        private final List<ExistenciasActualizadas> confirmados = new CopyOnWriteArrayList<>();

        @EventListener
        void alPublicar(ExistenciasActualizadas evento) {
            publicadosEnTransaccion.add(TransactionSynchronizationManager.isActualTransactionActive());
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void alConfirmar(ExistenciasActualizadas evento) {
            confirmados.add(evento);
        }

        void limpiar() {
            publicadosEnTransaccion.clear();
            confirmados.clear();
        }

        List<Boolean> publicadosEnTransaccion() {
            return publicadosEnTransaccion;
        }

        List<ExistenciasActualizadas> confirmados() {
            return confirmados;
        }
    }
}
