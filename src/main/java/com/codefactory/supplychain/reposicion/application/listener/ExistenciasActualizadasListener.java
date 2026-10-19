package com.codefactory.supplychain.reposicion.application.listener;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoMovimientoKardex;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.reposicion.application.port.in.TraspasoAutomaticoUseCase;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacciona a cambios de stock y dispara el traspaso automático si el nodo es un
 * Almacén de una tienda y el stock cayó bajo el umbral.
 * <p>
 * El evento ya trae {@code tipoNodo} y {@code tiendaId}, así que no hace falta
 * consultar los nodos.
 * <p>
 * Ignora movimientos de traspaso para evitar bucle: un traspaso genera
 * TRASPASO_SALIDA y TRASPASO_ENTRADA, que volverían a activar este listener.
 * <p>
 * Corre AFTER_COMMIT en su propia transacción (REQUIRES_NEW): si el traspaso
 * falla, la venta ya quedó registrada. Es asíncrono ({@code eventosReposicionExecutor})
 * para que el hilo de la venta libere su conexión antes de abrir la nueva
 * transacción; si no, con ventas concurrentes se agota el pool de conexiones.
 *
 * Módulo: reposicion.
 */
@Component("reposicionExistenciasActualizadasListener")
public class ExistenciasActualizadasListener {

    private final TraspasoAutomaticoUseCase traspasoAutomatico;

    public ExistenciasActualizadasListener(TraspasoAutomaticoUseCase traspasoAutomatico) {
        this.traspasoAutomatico = traspasoAutomatico;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async("eventosReposicionExecutor")
    public void onExistenciasActualizadas(ExistenciasActualizadas evento) {
        if (evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_SALIDA
                || evento.tipoMovimiento() == TipoMovimientoKardex.TRASPASO_ENTRADA) {
            return;
        }
        if (evento.tipoNodo() != TipoNodoInventario.ALMACEN || evento.tiendaId() == null) {
            return;
        }
        traspasoAutomatico.evaluar(evento.tiendaId(), evento.varianteId());
    }
}
