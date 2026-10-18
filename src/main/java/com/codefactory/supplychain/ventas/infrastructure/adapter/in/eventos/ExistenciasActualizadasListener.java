package com.codefactory.supplychain.ventas.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase.ExistenciasCambiadas;
import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * HU-36. El trabajo se despacha fuera del hilo que confirma Inventario para
 * liberar su conexión antes de abrir la transacción REQUIRES_NEW. Si falla, la
 * operación de Inventario queda confirmada y la reconciliación (HU-37) repara
 * la diferencia. Solo traduce el evento y delega en el caso de uso.
 */
@Component
public class ExistenciasActualizadasListener {

    private final GestionarVentanasQuiebreUseCase ventanas;

    public ExistenciasActualizadasListener(GestionarVentanasQuiebreUseCase ventanas) {
        this.ventanas = ventanas;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async("eventosInventarioExecutor")
    public void alActualizarExistencias(ExistenciasActualizadas evento) {
        ventanas.alCambiarExistencias(new ExistenciasCambiadas(evento.nodoId(), evento.varianteId(),
                evento.tipoMovimiento().name(), evento.forzado(), evento.aLaMano(), evento.ocurridoEn()));
    }
}
