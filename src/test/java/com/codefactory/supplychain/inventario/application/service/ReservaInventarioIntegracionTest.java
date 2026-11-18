package com.codefactory.supplychain.inventario.application.service;

import com.codefactory.supplychain.inventario.api.EstadoReserva;
import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.ReservasInventario;
import com.codefactory.supplychain.inventario.api.ResultadoReserva;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.inventario.application.port.in.InventarioUseCase;
import com.codefactory.supplychain.inventario.domain.model.MotivoAjuste;
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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HD-01 paso 2 contra PostgreSQL real: reservar y liberar a través de la API
 * pública, como las consume reposicion (HU-45, HU-53).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ReservaInventarioIntegracionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservasInventario reservas;

    @Autowired
    private InventarioUseCase inventarioUseCase;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EventosConfirmados eventos;

    private UUID cdId;
    private UUID nodoCd;
    private UUID varianteId;

    @BeforeEach
    void setUp() {
        cdId = UUID.randomUUID();
        jdbc.update("INSERT INTO cd (id, nombre, ubicacion) VALUES (?, ?, 'Bogotá')", cdId, "CD " + cdId);
        nodoCd = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, cd_id) VALUES (?, 'CD', ?)", nodoCd, cdId);
        varianteId = crearVariante();
        eventos.limpiar();
    }

    @Test
    @DisplayName("""
            Escenario: reserva con disponible suficiente en el CD
              Dado un CD con 10 unidades a la mano de un SKU
              Y una tienda cuya Bodega_Tienda nunca tuvo ese SKU
              Cuando se reservan 4 unidades para la tienda
              Entonces el resultado es ASIGNADO con disponible 6
              Y el CD queda con saliente 4 y disponible 6
              Y la Bodega_Tienda queda con una fila nueva con entrante 4 y pronosticado 4
              Y no se registra ningún movimiento en el kardex""")
    void reservaConDisponibleSuficiente() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        UUID tiendaId = UUID.randomUUID();
        UUID bodega = crearTiendaConBodega(tiendaId);
        int movimientosAntes = contarMovimientos();

        ResultadoReserva resultado = enTransaccion(() ->
                reservas.reservar(cdId, tiendaId, varianteId, 4, UUID.randomUUID()));

        assertThat(resultado.estado()).isEqualTo(EstadoReserva.ASIGNADO);
        assertThat(resultado.disponibleActual()).isEqualTo(6);
        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 10).containsEntry("saliente", 4)
                .containsEntry("disponible_para_uso", 6).containsEntry("pronosticado", 6);
        assertThat(fila(bodega)).containsEntry("a_la_mano", 0).containsEntry("entrante", 4)
                .containsEntry("pronosticado", 4);
        assertThat(contarMovimientos()).isEqualTo(movimientosAntes);
    }

    @Test
    @DisplayName("""
            Escenario: reserva publica un evento por fila sin movimiento de kardex
              Dado un CD con 10 unidades y una tienda con Bodega_Tienda
              Cuando se reservan 3 unidades
              Entonces tras el commit hay un evento del CD y uno de la Bodega_Tienda
              Y ambos tienen movimientoId y tipoMovimiento nulos y a_la_mano sin cambio
              Y ningún listener abre ventanas de quiebre ni crea traspasos""")
    void reservaPublicaEventosSinMovimiento() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        UUID tiendaId = UUID.randomUUID();
        crearTiendaConBodega(tiendaId);
        UUID ordenId = UUID.randomUUID();
        eventos.limpiar();

        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 3, ordenId));

        assertThat(eventos.recibidos()).hasSize(2);
        assertThat(eventos.recibidos()).allSatisfy(evento -> {
            assertThat(evento.movimientoId()).isNull();
            assertThat(evento.tipoMovimiento()).isNull();
            assertThat(evento.referenciaId()).isEqualTo(ordenId);
            assertThat(evento.aLaMano()).isEqualTo(evento.aLaManoAnterior());
        });
        assertThat(eventos.recibidos()).extracting(ExistenciasActualizadas::tipoNodo)
                .containsExactly(TipoNodoInventario.CD, TipoNodoInventario.BODEGA_TIENDA);
        assertThat(eventos.recibidos().get(0).saliente()).isEqualTo(3);
        assertThat(eventos.recibidos().get(1).entrante()).isEqualTo(3);
        assertThat(eventos.recibidos().get(1).tiendaId()).isEqualTo(tiendaId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM traspaso_interno WHERE tienda_id = ?",
                Integer.class, tiendaId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ventana_quiebre WHERE tienda_id = ?",
                Integer.class, tiendaId)).isZero();
    }

    @Test
    @DisplayName("""
            Escenario: el CD no cubre la cantidad
              Dado un CD con 3 unidades a la mano
              Cuando se intentan reservar 5 unidades
              Entonces el resultado es SIN_DISPONIBLE con disponible 3
              Y no cambia el CD, no se crea fila en la Bodega_Tienda y no se publica evento""")
    void sinDisponibleNoCambiaNada() {
        inventarioUseCase.cargar(nodoCd, varianteId, 3, null);
        UUID tiendaId = UUID.randomUUID();
        UUID bodega = crearTiendaConBodega(tiendaId);
        eventos.limpiar();

        ResultadoReserva resultado = enTransaccion(() ->
                reservas.reservar(cdId, tiendaId, varianteId, 5, UUID.randomUUID()));

        assertThat(resultado).isEqualTo(ResultadoReserva.sinDisponible(3));
        assertThat(fila(nodoCd)).containsEntry("saliente", 0).containsEntry("disponible_para_uso", 3);
        assertThat(existeFila(bodega)).isFalse();
        assertThat(eventos.recibidos()).isEmpty();
    }

    @Test
    @DisplayName("""
            Escenario: el CD nunca tuvo el SKU
              Dado un CD sin fila para el SKU
              Cuando se intenta reservar
              Entonces el resultado es SIN_DISPONIBLE con disponible 0 y no se crea ninguna fila""")
    void cdSinFilaRespondeSinDisponible() {
        UUID tiendaId = UUID.randomUUID();
        UUID bodega = crearTiendaConBodega(tiendaId);

        ResultadoReserva resultado = enTransaccion(() ->
                reservas.reservar(cdId, tiendaId, varianteId, 1, UUID.randomUUID()));

        assertThat(resultado).isEqualTo(ResultadoReserva.sinDisponible(0));
        assertThat(existeFila(nodoCd)).isFalse();
        assertThat(existeFila(bodega)).isFalse();
    }

    @Test
    @DisplayName("""
            Escenario: la reserva se une a la transacción del llamador
              Dado un CD con 10 unidades
              Cuando se reserva dentro de una transacción que se revierte
              Entonces ni el CD ni la Bodega_Tienda cambian y ningún evento se entrega""")
    void reservaSeRevierteConElLlamador() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        UUID tiendaId = UUID.randomUUID();
        UUID bodega = crearTiendaConBodega(tiendaId);
        eventos.limpiar();

        transactionTemplate.executeWithoutResult(estado -> {
            reservas.reservar(cdId, tiendaId, varianteId, 4, UUID.randomUUID());
            estado.setRollbackOnly();
        });

        assertThat(fila(nodoCd)).containsEntry("saliente", 0);
        assertThat(existeFila(bodega)).isFalse();
        assertThat(eventos.recibidos()).isEmpty();
    }

    @Test
    @DisplayName("""
            Escenario: liberar nunca deja valores negativos
              Dada una reserva de 4 unidades del CD para una tienda
              Cuando se liberan 10 de reserva y 10 de entrante
              Entonces el CD queda con saliente 0 y disponible igual a su a_la_mano
              Y la Bodega_Tienda queda con entrante 0""")
    void liberarNuncaDejaNegativos() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        UUID tiendaId = UUID.randomUUID();
        UUID bodega = crearTiendaConBodega(tiendaId);
        UUID ordenId = UUID.randomUUID();
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 4, ordenId));

        enTransaccion(() -> {
            reservas.liberar(cdId, tiendaId, varianteId, 10, 10, ordenId);
            return null;
        });

        assertThat(fila(nodoCd)).containsEntry("saliente", 0).containsEntry("disponible_para_uso", 10);
        assertThat(fila(bodega)).containsEntry("entrante", 0).containsEntry("pronosticado", 0);
    }

    @Test
    @DisplayName("""
            Escenario: liberar parcialmente la reserva y el entrante
              Dada una reserva de 6 unidades del CD para una tienda
              Cuando se liberan 2 de reserva y 5 de entrante
              Entonces el CD queda con saliente 4 y la Bodega_Tienda con entrante 1""")
    void liberarParcialmente() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        UUID tiendaId = UUID.randomUUID();
        UUID bodega = crearTiendaConBodega(tiendaId);
        UUID ordenId = UUID.randomUUID();
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 6, ordenId));

        enTransaccion(() -> {
            reservas.liberar(cdId, tiendaId, varianteId, 2, 5, ordenId);
            return null;
        });

        assertThat(fila(nodoCd)).containsEntry("saliente", 4).containsEntry("disponible_para_uso", 6);
        assertThat(fila(bodega)).containsEntry("entrante", 1);
    }

    @Test
    @DisplayName("""
            Escenario: un ajuste sobre stock reservado deja el disponible negativo (D-105)
              Dado un CD con 10 unidades y 8 reservadas
              Cuando un conteo físico baja el a_la_mano a 5
              Entonces el ajuste se acepta y el disponible queda en -3
              Y una nueva reserva responde SIN_DISPONIBLE""")
    void ajusteSobreStockReservadoDejaDisponibleNegativo() {
        inventarioUseCase.cargar(nodoCd, varianteId, 10, null);
        UUID tiendaId = UUID.randomUUID();
        crearTiendaConBodega(tiendaId);
        enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 8, UUID.randomUUID()));

        UUID usuarioId = jdbc.queryForObject("SELECT id FROM usuario LIMIT 1", UUID.class);
        inventarioUseCase.aplicarAjuste(nodoCd, varianteId, -5, MotivoAjuste.CONTEO_FISICO, null, usuarioId);

        assertThat(fila(nodoCd)).containsEntry("a_la_mano", 5).containsEntry("saliente", 8)
                .containsEntry("disponible_para_uso", -3);
        assertThat(enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 1, UUID.randomUUID())))
                .isEqualTo(ResultadoReserva.sinDisponible(-3));
    }

    @Test
    @DisplayName("""
            Escenario: la cantidad a reservar debe ser positiva
              Cuando se intenta reservar 0 unidades
              Entonces la operación se rechaza""")
    void cantidadNoPositivaSeRechaza() {
        UUID tiendaId = UUID.randomUUID();
        crearTiendaConBodega(tiendaId);

        assertThatThrownBy(() -> reservas.reservar(cdId, tiendaId, varianteId, 0, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor que cero");
    }

    @Test
    @DisplayName("""
            Escenario: reservas simultáneas nunca superan lo disponible (D-98)
              Dado un CD con 5 unidades de un SKU
              Cuando 12 tiendas reservan 1 unidad al mismo tiempo, cada una con reintento fuera de la transacción
              Entonces exactamente 5 reservas quedan ASIGNADO y 7 SIN_DISPONIBLE
              Y el saliente del CD es 5 y la suma de los entrantes de las Bodegas es 5""")
    void reservasSimultaneasNuncaSuperanLoDisponible() throws Exception {
        inventarioUseCase.cargar(nodoCd, varianteId, 5, null);
        int tiendas = 12;
        List<UUID> tiendaIds = new ArrayList<>();
        for (int i = 0; i < tiendas; i++) {
            UUID tiendaId = UUID.randomUUID();
            crearTiendaConBodega(tiendaId);
            tiendaIds.add(tiendaId);
        }
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(tiendas);
        List<Future<ResultadoReserva>> resultados = new ArrayList<>();
        try {
            for (UUID tiendaId : tiendaIds) {
                resultados.add(pool.submit(() -> {
                    salida.await();
                    return reservarConReintento(tiendaId);
                }));
            }
            salida.countDown();

            int asignadas = 0;
            int sinDisponible = 0;
            for (Future<ResultadoReserva> resultado : resultados) {
                if (resultado.get(60, TimeUnit.SECONDS).asignada()) {
                    asignadas++;
                } else {
                    sinDisponible++;
                }
            }

            assertThat(asignadas).isEqualTo(5);
            assertThat(sinDisponible).isEqualTo(7);
            assertThat(fila(nodoCd)).containsEntry("saliente", 5).containsEntry("disponible_para_uso", 0);
            assertThat(jdbc.queryForObject("""
                    SELECT coalesce(sum(i.entrante), 0) FROM inventario i
                      JOIN nodo n ON n.id = i.nodo_id
                     WHERE n.tipo = 'BODEGA_TIENDA' AND i.variante_id = ?""", Integer.class, varianteId))
                    .isEqualTo(5);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Simula al llamador: cada intento abre su propia transacción y el reintento va fuera (D-24). */
    private ResultadoReserva reservarConReintento(UUID tiendaId) throws InterruptedException {
        for (int intento = 1; ; intento++) {
            try {
                return enTransaccion(() -> reservas.reservar(cdId, tiendaId, varianteId, 1, UUID.randomUUID()));
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
                if (intento == 30) {
                    throw e;
                }
                Thread.sleep(ThreadLocalRandom.current().nextLong(5, 25));
            }
        }
    }

    private <T> T enTransaccion(java.util.function.Supplier<T> operacion) {
        return transactionTemplate.execute(estado -> operacion.get());
    }

    private UUID crearTiendaConBodega(UUID tiendaId) {
        jdbc.update("INSERT INTO tienda (id, nombre, ubicacion) VALUES (?, ?, 'Medellín')",
                tiendaId, "Tienda Reserva " + tiendaId);
        UUID bodega = UUID.randomUUID();
        jdbc.update("INSERT INTO nodo (id, tipo, tienda_id) VALUES (?, 'BODEGA_TIENDA', ?)", bodega, tiendaId);
        return bodega;
    }

    private UUID crearVariante() {
        UUID categoriaId = UUID.randomUUID();
        jdbc.update("INSERT INTO categoria (id, nombre) VALUES (?, ?)", categoriaId, "Categoría Reserva " + categoriaId);
        UUID templateId = UUID.randomUUID();
        jdbc.update("INSERT INTO template (id, categoria_id, nombre) VALUES (?, ?, ?)",
                templateId, categoriaId, "Template Reserva " + templateId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO variante (id, template_id, sku) VALUES (?, ?, ?)", id, templateId, "SKU-" + id);
        return id;
    }

    private java.util.Map<String, Object> fila(UUID nodoId) {
        return jdbc.queryForMap("""
                SELECT a_la_mano, entrante, saliente, disponible_para_uso, pronosticado
                  FROM inventario WHERE nodo_id = ? AND variante_id = ?""", nodoId, varianteId);
    }

    private boolean existeFila(UUID nodoId) {
        return jdbc.queryForObject("SELECT count(*) FROM inventario WHERE nodo_id = ? AND variante_id = ?",
                Integer.class, nodoId, varianteId) > 0;
    }

    private int contarMovimientos() {
        return jdbc.queryForObject("SELECT count(*) FROM movimiento_inventario WHERE variante_id = ?",
                Integer.class, varianteId);
    }

    @TestConfiguration
    static class ConfiguracionEventos {

        @Bean
        EventosConfirmados eventosConfirmados() {
            return new EventosConfirmados();
        }
    }

    /** Captura los ExistenciasActualizadas de la variante de la prueba que llegan tras el commit. */
    static class EventosConfirmados {

        private final List<ExistenciasActualizadas> recibidos = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void alConfirmar(ExistenciasActualizadas evento) {
            recibidos.add(evento);
        }

        void limpiar() {
            recibidos.clear();
        }

        List<ExistenciasActualizadas> recibidos() {
            return recibidos;
        }
    }
}
