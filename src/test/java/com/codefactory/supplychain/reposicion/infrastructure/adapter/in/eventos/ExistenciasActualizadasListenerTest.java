package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("ExistenciasActualizadasListener")
@ExtendWith(MockitoExtension.class)
class ExistenciasActualizadasListenerTest {

    @Mock TraspasoAutomaticoUseCase traspasoAutomatico;

    @InjectMocks
    ExistenciasActualizadasListener listener;

    private static final UUID TIENDA_ID   = UUID.randomUUID();
    private static final UUID NODO_ID     = UUID.randomUUID();
    private static final UUID VARIANTE_ID = UUID.randomUUID();

    @Nested
    @DisplayName("cuando el nodo es de tipo ALMACEN y hay tiendaId")
    class NodoAlmacenConTienda {

        @Test
        @DisplayName("llama al caso de uso")
        void llamaCasoDeUsoParaAlmacen() {
            when(traspasoAutomatico.evaluar(TIENDA_ID, VARIANTE_ID)).thenReturn(Optional.empty());

            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.ALMACEN, TIENDA_ID, TipoMovimientoKardex.VENTA));

            verify(traspasoAutomatico).evaluar(TIENDA_ID, VARIANTE_ID);
        }

        @Test
        @DisplayName("tolera que el caso de uso devuelva traspaso ejecutado (no lanza)")
        void toleraTraspasoEjecutado() {
            TraspasoInterno traspaso = TraspasoInterno.reconstruir(
                    UUID.randomUUID(), VARIANTE_ID, TIENDA_ID, 7,
                    OrigenTraspaso.AUTOMATICO, null, Instant.now());
            when(traspasoAutomatico.evaluar(TIENDA_ID, VARIANTE_ID)).thenReturn(Optional.of(traspaso));

            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.ALMACEN, TIENDA_ID, TipoMovimientoKardex.VENTA));

            verify(traspasoAutomatico).evaluar(TIENDA_ID, VARIANTE_ID);
        }
    }

    @Nested
    @DisplayName("cuando el nodo no es de tipo ALMACEN")
    class NodoNoAlmacen {

        @Test
        @DisplayName("ignora una disminución en la Bodega")
        void ignoraDisminucionEnBodega() {
            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.BODEGA_TIENDA, TIENDA_ID, TipoMovimientoKardex.AJUSTE));

            verifyNoInteractions(traspasoAutomatico);
        }

        @Test
        @DisplayName("evalúa la tienda ante un aumento en la Bodega (D-53)")
        void evaluaAnteAumentoEnBodega() {
            listener.onExistenciasActualizadas(evento(TipoNodoInventario.BODEGA_TIENDA, TIENDA_ID,
                    TipoMovimientoKardex.CARGA_INICIAL, 0, 8));

            verify(traspasoAutomatico).evaluar(TIENDA_ID, VARIANTE_ID);
        }

        @Test
        @DisplayName("ignora eventos de nodo tipo CD")
        void ignoraEventosDeCD() {
            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.CD, null, TipoMovimientoKardex.VENTA));

            verifyNoInteractions(traspasoAutomatico);
        }
    }

    @Nested
    @DisplayName("cuando el nodo es ALMACEN pero no tiene tiendaId")
    class AlmacenSinTienda {

        @Test
        @DisplayName("no llama al caso de uso")
        void ignoraAlmacenSinTiendaId() {
            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.ALMACEN, null, TipoMovimientoKardex.VENTA));

            verifyNoInteractions(traspasoAutomatico);
        }
    }

    @Nested
    @DisplayName("cuando el movimiento es de tipo TRASPASO (anti-bucle D-55)")
    class MovimientoDeTraspaso {

        @Test
        @DisplayName("ignora TRASPASO_SALIDA")
        void ignoraTraspasoSalida() {
            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.ALMACEN, TIENDA_ID, TipoMovimientoKardex.TRASPASO_SALIDA));

            verifyNoInteractions(traspasoAutomatico);
        }

        @Test
        @DisplayName("ignora TRASPASO_ENTRADA")
        void ignoraTraspasoEntrada() {
            listener.onExistenciasActualizadas(
                    evento(TipoNodoInventario.ALMACEN, TIENDA_ID, TipoMovimientoKardex.TRASPASO_ENTRADA));

            verifyNoInteractions(traspasoAutomatico);
        }
    }

    private static ExistenciasActualizadas evento(TipoNodoInventario tipoNodo, UUID tiendaId,
                                                  TipoMovimientoKardex tipoMovimiento) {
        return evento(tipoNodo, tiendaId, tipoMovimiento, 12, 10);
    }

    private static ExistenciasActualizadas evento(TipoNodoInventario tipoNodo, UUID tiendaId,
                                                  TipoMovimientoKardex tipoMovimiento,
                                                  int aLaManoAnterior, int aLaMano) {
        return new ExistenciasActualizadas(
                NODO_ID,            // nodoId
                tipoNodo,           // tipoNodo
                tiendaId,           // tiendaId
                VARIANTE_ID,        // varianteId
                UUID.randomUUID(),  // movimientoId
                tipoMovimiento,     // tipoMovimiento
                false,              // forzado
                null,               // referenciaId
                aLaManoAnterior,    // aLaManoAnterior
                aLaMano,            // aLaMano
                3,                  // disponibleParaUso
                0,                  // entrante
                0,                  // saliente
                0,                  // pronosticado
                Instant.now());     // ocurridoEn
    }
}
