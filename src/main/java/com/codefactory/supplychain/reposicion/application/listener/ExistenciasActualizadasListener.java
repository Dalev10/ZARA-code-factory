package com.codefactory.supplychain.reposicion.application.listener;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import com.codefactory.supplychain.reposicion.application.port.out.ConsultarNodosPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacciona a cambios de stock y dispara el traspaso automático si el nodo es un
 * Almacén y el stock cayó bajo el umbral.
 * <p>
 * Ignora movimientos de traspaso para evitar bucle: un traspaso genera
 * TRASPASO_SALIDA y TRASPASO_ENTRADA, que volverían a activar este listener.
 * <p>
 * Corre AFTER_COMMIT en su propia transacción (REQUIRES_NEW): si el traspaso
 * falla, la venta ya quedó registrada.
 *
 * Módulo: reposicion.
 */
@Component
public class ExistenciasActualizadasListener {

    private final ConsultarNodosPort consultarNodos;
    private final TraspasoAutomaticoUseCase traspasoAutomatico;

    public ExistenciasActualizadasListener(ConsultarNodosPort consultarNodos,
                                           TraspasoAutomaticoUseCase traspasoAutomatico) {
        this.consultarNodos = consultarNodos;
        this.traspasoAutomatico = traspasoAutomatico;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExistenciasActualizadas(ExistenciasActualizadas evento) {
        if (evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_SALIDA
                || evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_ENTRADA) {
            return;
        }
        consultarNodos.buscarTiendaPorNodoAlmacen(evento.nodoId())
                .ifPresent(tiendaId -> traspasoAutomatico.evaluar(tiendaId, evento.varianteId()));
    }
}
