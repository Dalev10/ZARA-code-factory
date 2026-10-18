package com.codefactory.supplychain.reposicion.application.listener;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.domain.model.OrigenTraspaso;
import com.codefactory.supplychain.reposicion.domain.model.TraspasoInterno;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@DisplayName("ExistenciasActualizadasListener")
@ExtendWith(MockitoExtension.class)
class ExistenciasActualizadasListenerTest {

    @Mock TraspasoAutomaticoUseCase traspasoAutomatico;

    @InjectMocks
    ExistenciasActualizadasListener listener;

    private static final UUID TIENDA_ID   = UUID.randomUUID();
    private static final UUID NODO_ID     = UUID.randomUUID();
    private static final UUID VARIANTE_ID = UUID.randomUUID();

    @Test
    @DisplayName("llama al caso de uso cuando el evento es del tipo ALMACEN")
    void llamaCasoDeUsoParaAlmacen() {
        ExistenciasActualizadas evento = eventoDeAlmacen("ALMACEN");
        when(traspasoAutomatico.evaluar(TIENDA_ID, VARIANTE_ID)).thenReturn(Optional.empty());

        listener.onExistenciasActualizadas(evento);

        verify(traspasoAutomatico).evaluar(TIENDA_ID, VARIANTE_ID);
    }

    @Test
    @DisplayName("ignora eventos de nodo tipo BODEGA_TIENDA")
    void ignoraEventosDeBodega() {
        ExistenciasActualizadas evento = eventoDeAlmacen("BODEGA_TIENDA");

        listener.onExistenciasActualizadas(evento);

        verifyNoInteractions(traspasoAutomatico);
    }

    @Test
    @DisplayName("ignora eventos de nodo tipo CD")
    void ignoraEventosDeCD() {
        ExistenciasActualizadas evento = eventoDeAlmacen("CD");

        listener.onExistenciasActualizadas(evento);

        verifyNoInteractions(traspasoAutomatico);
    }

    @Test
    @DisplayName("ignora eventos de ALMACEN sin tiendaId (nodo de CD)")
    void ignoraAlmacenSinTiendaId() {
        ExistenciasActualizadas evento = new ExistenciasActualizadas(
                NODO_ID, VARIANTE_ID, UUID.randomUUID(), TipoMovimientoKardex.VENTA, null, 10, 3, 0, 0, 0, Instant.now()
        );

        listener.onExistenciasActualizadas(evento);

        verifyNoInteractions(traspasoAutomatico);
    }

    @Test
    @DisplayName("tolera que el caso de uso devuelva traspaso ejecutado (no lanza)")
    void toleraTraspasoEjecutado() {
        ExistenciasActualizadas evento = eventoDeAlmacen("ALMACEN");
        TraspasoInterno traspaso = TraspasoInterno.reconstruir(
                UUID.randomUUID(), VARIANTE_ID, TIENDA_ID, 7,
                OrigenTraspaso.AUTOMATICO, null, Instant.now());
        when(traspasoAutomatico.evaluar(TIENDA_ID, VARIANTE_ID))
                .thenReturn(Optional.of(traspaso));

        listener.onExistenciasActualizadas(evento);

        verify(traspasoAutomatico).evaluar(TIENDA_ID, VARIANTE_ID);
    }

    // ---- helpers ----

    private ExistenciasActualizadas eventoDeAlmacen(String tipoNodo) {
        UUID tiendaId = "ALMACEN".equals(tipoNodo) || "BODEGA_TIENDA".equals(tipoNodo)
                ? TIENDA_ID : null;
        return new ExistenciasActualizadas(
                NODO_ID, VARIANTE_ID, tiendaId, TipoMovimientoKardex.VENTA, null, 10, 3, 0, 0, 0, Instant.now()
        );
    }
}
