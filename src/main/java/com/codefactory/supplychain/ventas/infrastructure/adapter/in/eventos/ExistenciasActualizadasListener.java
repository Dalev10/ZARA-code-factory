package com.codefactory.supplychain.ventas.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase;
import com.codefactory.supplychain.ventas.application.port.in.GestionarVentanasQuiebreUseCase.ExistenciasCambiadas;
import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * HU-45. Corre AFTER_COMMIT, en el mismo hilo y en su propia transacción (D-16): la
 * ventana ya se ve al recibir la respuesta (D-18). Si falla, la operación de
 * Inventario queda confirmada (D-17) y la reconciliación (HU-46) repara la
 * diferencia. Solo traduce el evento y delega en el caso de uso.
 */
@Component
public class ExistenciasActualizadasListener {

    private final GestionarVentanasQuiebreUseCase ventanas;

    public ExistenciasActualizadasListener(GestionarVentanasQuiebreUseCase ventanas) {
        this.ventanas = ventanas;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alActualizarExistencias(ExistenciasActualizadas evento) {
        UUID tiendaId = evento.tipoNodo() == TipoNodoInventario.ALMACEN ? evento.tiendaId() : null;
        ventanas.alCambiarExistencias(new ExistenciasCambiadas(tiendaId, evento.varianteId(), evento.forzado(),
                evento.aLaManoAnterior(), evento.aLaMano(), evento.ocurridoEn()));
    }
}
