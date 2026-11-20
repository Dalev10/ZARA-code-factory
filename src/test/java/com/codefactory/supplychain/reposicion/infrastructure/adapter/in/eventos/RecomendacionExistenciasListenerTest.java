package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.reposicion.application.port.in.EvaluarRecomendacionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RecomendacionExistenciasListener")
class RecomendacionExistenciasListenerTest {

    private final EvaluarRecomendacionUseCase evaluar = mock(EvaluarRecomendacionUseCase.class);
    private final RecomendacionExistenciasListener listener = new RecomendacionExistenciasListener(evaluar);

    private final UUID tiendaId = UUID.randomUUID();
    private final UUID varianteId = UUID.randomUUID();

    @Test
    @DisplayName("declara @Order(30), AFTER_COMMIT y REQUIRES_NEW")
    void declaraOrdenYTransaccion() throws Exception {
        var metodo = RecomendacionExistenciasListener.class
                .getMethod("onExistenciasActualizadas", ExistenciasActualizadas.class);

        assertThat(metodo.getAnnotation(Order.class).value()).isEqualTo(30);
        assertThat(metodo.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(metodo.getAnnotation(Transactional.class).propagation())
                .isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("corre después de ventas (10) y del traspaso (20)")
    void ordenEsperadoFrenteALosDemasListeners() {
        int ventas = 10;
        int traspaso = 20;
        assertThat(RecomendacionExistenciasListener.ORDEN).isGreaterThan(ventas).isGreaterThan(traspaso);
    }

    @Test
    @DisplayName("reacciona a TRASPASO_SALIDA de la Bodega")
    void reaccionaATraspasoSalida() {
        listener.onExistenciasActualizadas(evento(TipoNodoInventario.BODEGA_TIENDA, TipoMovimientoKardex.TRASPASO_SALIDA));
        verify(evaluar).evaluar(tiendaId, varianteId);
    }

    @Test
    @DisplayName("tolera eventos sin movimiento (tipoMovimiento nulo, cambio de entrante)")
    void toleraTipoMovimientoNulo() {
        listener.onExistenciasActualizadas(evento(TipoNodoInventario.BODEGA_TIENDA, null));
        verify(evaluar).evaluar(tiendaId, varianteId);
    }

    @Test
    @DisplayName("ignora eventos del Almacén y del CD")
    void ignoraOtrosNodos() {
        listener.onExistenciasActualizadas(evento(TipoNodoInventario.ALMACEN, TipoMovimientoKardex.VENTA));
        listener.onExistenciasActualizadas(evento(TipoNodoInventario.CD, null));
        verifyNoInteractions(evaluar);
    }

    @Test
    @DisplayName("una falla de la evaluación se registra y no se propaga")
    void fallaNoSePropaga() {
        when(evaluar.evaluar(any(), any())).thenThrow(new IllegalStateException("falla"));
        assertThatCode(() -> listener.onExistenciasActualizadas(
                evento(TipoNodoInventario.BODEGA_TIENDA, TipoMovimientoKardex.VENTA))).doesNotThrowAnyException();
    }

    private ExistenciasActualizadas evento(TipoNodoInventario nodo, TipoMovimientoKardex tipo) {
        UUID tienda = nodo == TipoNodoInventario.CD ? null : tiendaId;
        return new ExistenciasActualizadas(UUID.randomUUID(), nodo, tienda, varianteId,
                tipo == null ? null : UUID.randomUUID(), tipo, false, null,
                5, 4, 4, 0, 0, 4, Instant.now());
    }
}
