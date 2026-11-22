package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.MovimientosAbastecimiento;
import com.codefactory.supplychain.inventario.api.ReservasInventario;
import com.codefactory.supplychain.inventario.api.StockInsuficienteException;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HD-01 paso 3 contra PostgreSQL real: registrarDespacho y registrarRecepcion a
 * través de la API pública, como las consume logistica (HU-46, HU-49).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AbastecimientoInventarioIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MovimientosAbastecimiento movimientos;

    @Autowired
    private ReservasInventario reservas;

    @Autowired
    private InventarioUseCase inventarioUseCase;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EventosAbastecimiento eventos;

    private UUID cdId;
    private UUID nodoCd;
    private UUID tiendaId;
    private UUID bodega;
    private UUID varianteId;

    @BeforeEach
    void setUp() {
        cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        nodoCd = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodoCd, cdId);
        tiendaId = UUID.randomUUID();
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda Abastecimiento " + tiendaId);
        bodega = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", bodega, tiendaId);
        varianteId = crearVariante();
        eventos.limpiar();
    }

    @Test
    @DisplayName("""
            Escenario: despacho total de una orden reservada
              Dado un CD con 10 unidades y 6 reservadas para la tienda
              Cuando se despachan 6 unidades liberando 6 de reserva
              Entonces el CD queda con a_la_mano 4, saliente 0 y disponible 4
              Y el kardex del CD tiene un DESPACHO de -6 con el ciclo como referencia
              Y la Bodega_Tienda no cambia: el entrante sigue en 6""")
    void despachoTotal() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 6, UUID.randomUUID()));
        UUID ciclo = UUID.randomUUID();

        enTransaccion(() -> movimientos.registrarDespacho(cdId, varianteId, 6, 6, ciclo));

        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 4).containsEntry("saliente", 0)
                .containsEntry("disponible_para_uso", 4);
        assertThat(kardex(nodoCd)).containsExactly(Map.of("tipo", "DESPACHO", "cantidad", -6, "referencia_id", ciclo));
        assertThat(fila(bodega)).containsEntry("a_la_mano", 0).containsEntry("entrante", 6);
    }

    @Test
    @DisplayName("""
            Escenario: despacho sin stock suficiente a la mano
              Dado un CD con 3 unidades a la mano
              Cuando se intentan despachar 4 unidades
              Entonces se lanza StockInsuficienteException de la API
              Y el CD no cambia ni se registra movimiento""")
    void despachoSinStockSuficiente() {
        inventarioUseCase.cargar(nodoCd, varianteId, 3, null);

        assertThatThrownBy(() -> enTransaccion(() ->
                movimientos.registrarDespacho(cdId, varianteId, 4, 0, UUID.randomUUID())))
                .isInstanceOf(StockInsuficienteException.class);

        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 3);
        assertThat(kardex(nodoCd)).isEmpty();
    }

    @Test
    @DisplayName("""
            Escenario: recepción de una variante que nunca estuvo en la Bodega_Tienda (D-104)
              Dada una Bodega_Tienda sin fila para el SKU
              Cuando se reciben 5 unidades
              Entonces se crea la fila con a_la_mano 5 y entrante 0
              Y el kardex de la Bodega_Tienda tiene una RECEPCION de 5 con la recepción como referencia""")
    void recepcionCreaLaFilaSiFalta() {
        UUID recepcion = UUID.randomUUID();

        enTransaccion(() -> movimientos.registrarRecepcion(tiendaId, varianteId, 5, recepcion));

        assertThat(fila(bodega)).containsEntry("a_la_mano", 5).containsEntry("entrante", 0)
                .containsEntry("pronosticado", 5);
        assertThat(kardex(bodega)).containsExactly(
                Map.of("tipo", "RECEPCION", "cantidad", 5, "referencia_id", recepcion));
    }

    @Test
    @DisplayName("""
            Escenario: despacho y recepción publican el evento con su movimiento
              Dada una reserva de 4 unidades del CD para la tienda
              Cuando se despachan 4 y se reciben 4
              Entonces hay un evento DESPACHO del CD y uno RECEPCION de la Bodega_Tienda
              Y el de la Bodega_Tienda refleja el aumento de a_la_mano y el entrante en 0""")
    void despachoYRecepcionPublicanEventoConMovimiento() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 4, UUID.randomUUID()));
        eventos.limpiar();

        enTransaccion(() -> movimientos.registrarDespacho(cdId, varianteId, 4, 4, UUID.randomUUID()));
        enTransaccion(() -> movimientos.registrarRecepcion(tiendaId, varianteId, 4, UUID.randomUUID()));

        List<ExistenciasActualizadas> recibidos = eventos.de(varianteId);
        assertThat(recibidos).extracting(ExistenciasActualizadas::tipoMovimiento)
                .containsExactly(TipoMovimientoKardex.DESPACHO, TipoMovimientoKardex.RECEPCION);
        assertThat(recibidos.get(0).tipoNodo()).isEqualTo(TipoNodoInventario.CD);
        assertThat(recibidos.get(0).aLaManoAnterior()).isEqualTo(10);
        assertThat(recibidos.get(0).aLaMano()).isEqualTo(6);
        assertThat(recibidos.get(0).saliente()).isZero();
        ExistenciasActualizadas recepcion = recibidos.get(1);
        assertThat(recepcion.tipoNodo()).isEqualTo(TipoNodoInventario.BODEGA_TIENDA);
        assertThat(recepcion.tiendaId()).isEqualTo(tiendaId);
        assertThat(recepcion.aLaManoAnterior()).isZero();
        assertThat(recepcion.aLaMano()).isEqualTo(4);
        assertThat(recepcion.entrante()).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: cuadre de una orden con despacho parcial, pérdida en tránsito y reapertura (2.5)
              Dado un CD con 10 unidades y una orden de 6 unidades reservada
              Cuando se despachan 4, se reciben 3, se despachan 3 más y se reciben 3
              Entonces tras cada paso la reserva es max(S − ΣD, 0) y el entrante es S − ΣR
              Y al final el CD queda con a_la_mano 3 y saliente 0, y la Bodega con a_la_mano 6 y entrante 0""")
    void cuadreConDespachoParcialYReapertura() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        int s = 6;
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, s, UUID.randomUUID()));

        // Ciclo 1: despacho parcial de 4; reserva restante = 6 - 0 = 6, se liberan min(4, 6) = 4.
        enTransaccion(() -> movimientos.registrarDespacho(cdId, varianteId, 4, 4, UUID.randomUUID()));
        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 6).containsEntry("saliente", 2);
        // Se reciben 3 de 4: 1 unidad es pérdida en tránsito.
        enTransaccion(() -> movimientos.registrarRecepcion(tiendaId, varianteId, 3, UUID.randomUUID()));
        assertThat(fila(bodega)).containsEntry("a_la_mano", 3).containsEntry("entrante", s - 3);

        // Ciclo 2 tras reapertura: pendiente 3; reserva restante = max(6 - 4, 0) = 2.
        enTransaccion(() -> movimientos.registrarDespacho(cdId, varianteId, 3, 2, UUID.randomUUID()));
        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 3).containsEntry("saliente", 0)
                .containsEntry("disponible_para_uso", 3);
        enTransaccion(() -> movimientos.registrarRecepcion(tiendaId, varianteId, 3, UUID.randomUUID()));

        assertThat(fila(bodega)).containsEntry("a_la_mano", 6).containsEntry("entrante", 0)
                .containsEntry("pronosticado", 6);
    }

    @Test
    @DisplayName("""
            Escenario: el despacho se une a la transacción del llamador
              Dado un CD con 5 unidades
              Cuando se despachan 2 dentro de una transacción que se revierte
              Entonces el CD sigue con 5 y no queda movimiento""")
    void despachoSeRevierteConElLlamador() {
        inventarioUseCase.cargar(nodoCd, varianteId, 5, null);

        transactionTemplate.executeWithoutResult(estado -> {
            movimientos.registrarDespacho(cdId, varianteId, 2, 0, UUID.randomUUID());
            estado.setRollbackOnly();
        });

        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 5);
        assertThat(kardex(nodoCd)).isEmpty();
    }

    @Test
    @DisplayName("""
            Escenario: despachos simultáneos sobre el mismo SKU del CD
              Dado un CD con 5 unidades a la mano
              Cuando 8 despachos de 1 unidad llegan al mismo tiempo, cada uno con reintento fuera de la transacción
              Entonces exactamente 5 se registran y 3 fallan por stock insuficiente
              Y el CD queda en 0 con 5 movimientos DESPACHO""")
    void despachosSimultaneos() throws Exception {
        inventarioUseCase.cargar(nodoCd, varianteId, 5, null);

        List<Boolean> resultados = ejecutarSimultaneamente(8,
                () -> movimientos.registrarDespacho(cdId, varianteId, 1, 0, UUID.randomUUID()));

        assertThat(resultados).filteredOn(exito -> exito).hasSize(5);
        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 0);
        assertThat(kardex(nodoCd)).hasSize(5);
    }

    @Test
    @DisplayName("""
            Escenario: recepciones simultáneas en la misma Bodega_Tienda
              Dada una reserva previa que dejó 10 unidades de entrante en la Bodega_Tienda
              Cuando 10 recepciones de 1 unidad llegan al mismo tiempo, cada una con reintento fuera de la transacción
              Entonces todas se registran: a_la_mano 10, entrante 0 y 10 movimientos RECEPCION""")
    void recepcionesSimultaneas() throws Exception {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 10, UUID.randomUUID()));

        List<Boolean> resultados = ejecutarSimultaneamente(10,
                () -> movimientos.registrarRecepcion(tiendaId, varianteId, 1, UUID.randomUUID()));

        assertThat(resultados).containsOnly(true);
        assertThat(fila(bodega)).containsEntry("a_la_mano", 10).containsEntry("entrante", 0);
        assertThat(kardex(bodega)).hasSize(10);
    }

    /**
     * Lanza {@code hilos} operaciones a la vez. Cada intento abre su propia
     * transacción y el reintento va fuera (D-24), como lo hará el llamador.
     * Devuelve true por operación registrada y false por stock insuficiente.
     */
    private List<Boolean> ejecutarSimultaneamente(int hilos, Runnable operacion) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Future<Boolean>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < hilos; i++) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return conReintento(operacion);
                }));
            }
            salida.countDown();
            List<Boolean> resultados = new ArrayList<>();
            for (Future<Boolean> futuro : futuros) {
                try {
                    resultados.add(futuro.get(60, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    throw new AssertionError("Falla inesperada en una operación concurrente", e.getCause());
                }
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    private boolean conReintento(Runnable operacion) throws InterruptedException {
        for (int intento = 1; ; intento++) {
            try {
                enTransaccion(operacion);
                return true;
            } catch (StockInsuficienteException e) {
                return false;
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                if (intento == 30) {
                    throw e;
                }
                Thread.sleep(ThreadLocalRandom.current().nextLong(5, 25));
            }
        }
    }

    private void enTransaccion(Runnable operacion) {
        transactionTemplate.executeWithoutResult(estado -> operacion.run());
    }

    private UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría Abast " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template Abast " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", id, templateId, "SKU-" + id);
        return id;
    }

    private Map<String, Object> fila(UUID nodoId) {
        return jdbc.queryForMap("""
                SELECT a_la_mano, entrante, saliente, disponible_para_uso, pronosticado
                  FROM inventario WHERE nodo_id = ? AND variante_id = ?""", nodoId, varianteId);
    }

    /** Movimientos DESPACHO y RECEPCION del nodo, en orden. */
    private List<Map<String, Object>> kardex(UUID nodoId) {
        return jdbc.queryForList("""
                SELECT tipo, cantidad, referencia_id FROM movimiento_inventario
                 WHERE nodo_id = ? AND variante_id = ? AND tipo IN ('DESPACHO', 'RECEPCION')
                 ORDER BY ocurrido_en, id""", nodoId, varianteId);
    }

    @TestConfiguration
    static class ConfiguracionEventos {

        @Bean
        EventosAbastecimiento eventosAbastecimiento() {
            return new EventosAbastecimiento();
        }
    }

    /** Captura los ExistenciasActualizadas de DESPACHO y RECEPCION que llegan tras el commit. */
    static class EventosAbastecimiento {

        private final List<ExistenciasActualizadas> recibidos = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void alConfirmar(ExistenciasActualizadas evento) {
            if (evento.tipoMovimiento() == TipoMovimientoKardex.DESPACHO
                    || evento.tipoMovimiento() == TipoMovimientoKardex.RECEPCION) {
                recibidos.add(evento);
            }
        }

        void limpiar() {
            recibidos.clear();
        }

        List<ExistenciasActualizadas> de(UUID varianteId) {
            return recibidos.stream().filter(evento -> evento.varianteId().equals(varianteId)).toList();
        }
    }
}
