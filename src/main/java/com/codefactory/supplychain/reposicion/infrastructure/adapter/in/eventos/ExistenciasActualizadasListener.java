package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacciona a cambios de stock y evalúa el traspaso automático de la tienda (D-53):
 * ante cualquier movimiento del Almacén y ante aumentos en la Bodega, que pueden
 * reponer un Almacén que antes quedó bajo el mínimo sin stock para traspasar.
 * <p>
 * El evento ya trae {@code tipoNodo} y {@code tiendaId}, así que no hace falta
 * consultar los nodos.
 * <p>
 * Ignora movimientos de traspaso para evitar bucle: un traspaso genera
 * TRASPASO_SALIDA y TRASPASO_ENTRADA, que volverían a activar este listener.
 * <p>
 * Corre AFTER_COMMIT, en el mismo hilo y en su propia transacción (D-16): el
 * traspaso ya se ve al recibir la respuesta de la venta (D-18) y, si falla, la
 * venta queda registrada igual (D-17).
 *
 * Módulo: reposicion — HU-48.
 */
@Component("reposicionExistenciasActualizadasListener")
public class ExistenciasActualizadasListener {

    private final TraspasoAutomaticoUseCase traspasoAutomatico;

    public ExistenciasActualizadasListener(TraspasoAutomaticoUseCase traspasoAutomatico) {
        this.traspasoAutomatico = traspasoAutomatico;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExistenciasActualizadas(ExistenciasActualizadas evento) {
        if (evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_SALIDA
                || evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_ENTRADA) {
            return;
        }
        boolean almacen = evento.tipoNodo() == TipoNodoInventario.ALMACEN;
        boolean aumentoEnBodega = evento.tipoNodo() == TipoNodoInventario.BODEGA_TIENDA
                && evento.aLaMano() > evento.aLaManoAnterior();
        if (evento.tiendaId() == null || !(almacen || aumentoEnBodega)) {
            return;
        }
        traspasoAutomatico.evaluar(evento.tiendaId(), evento.varianteId());
    }
}
