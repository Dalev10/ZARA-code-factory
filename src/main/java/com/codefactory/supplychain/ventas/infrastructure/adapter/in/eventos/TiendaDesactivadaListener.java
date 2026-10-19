package com.codefactory.supplychain.ventas.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.inventario.api.TiendaDesactivada;
import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * HU-45 (D-50): al desactivar una tienda se cierran sus ventanas abiertas con
 * TIENDA_DESACTIVADA. Al reactivarla no se reabren. Si falla, la desactivación queda
 * confirmada igual (D-17).
 */
@Component
public class TiendaDesactivadaListener {

    private final GestionarVentanasQuiebreUseCase ventanas;

    public TiendaDesactivadaListener(GestionarVentanasQuiebreUseCase ventanas) {
        this.ventanas = ventanas;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alDesactivarTienda(TiendaDesactivada evento) {
        ventanas.alDesactivarTienda(evento.tiendaId(), evento.fecha());
    }
}
