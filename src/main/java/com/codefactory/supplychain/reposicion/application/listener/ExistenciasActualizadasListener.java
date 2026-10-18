package com.codefactory.supplychain.reposicion.application.listener;

import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.ConsultaNodos;

/**
 * Reacciona a cambios de stock del Almacén y dispara el traspaso automático si
 * corresponde.
 * 
 * Solo le interesan los movimientos del nodo ALMACEN; los de Bodega o CD los ignora.
 * El listener corre AFTER_COMMIT en su propia transacción (REQUIRES_NEW): si el
 * traspaso falla, la operación de origen —la venta— ya quedó registrada.
 * 
 * Tolerante a eventos duplicados: si el traspaso ya no aplica (umbral cubierto o
 * bodega vacía) el caso de uso devuelve vacío sin efecto.
 *
 * Módulo: reposicion — Reposición interna.
 */
@Component
public class ExistenciasActualizadasListener {

    private final ConsultaNodos consultaNodos;
    private final TraspasoAutomaticoUseCase traspasoAutomatico;

    public ExistenciasActualizadasListener(ConsultaNodos consultaNodos,
                                            TraspasoAutomaticoUseCase traspasoAutomatico) {
        this.consultaNodos = consultaNodos;
        this.traspasoAutomatico = traspasoAutomatico;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExistenciasActualizadas(ExistenciasActualizadas evento) {
        if (evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_SALIDA
                || evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_ENTRADA) {
            return;
        }
        consultaNodos.buscarTiendaPorNodoAlmacen(evento.nodoId()).ifPresent(tiendaId ->
            traspasoAutomatico.evaluar(tiendaId, evento.varianteId())
        );
    }
}
