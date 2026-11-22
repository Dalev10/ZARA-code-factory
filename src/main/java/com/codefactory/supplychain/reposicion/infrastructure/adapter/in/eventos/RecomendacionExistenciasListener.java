package com.codefactory.supplychain.reposicion.infrastructure.adapter.in.eventos;

import com.codefactory.supplychain.inventario.api.ExistenciasActualizadas;
import com.codefactory.supplychain.inventario.api.TipoNodoInventario;
import com.codefactory.supplychain.reposicion.application.port.in.EvaluarRecomendacionUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * HU-43: evalúa la recomendación de reposición al CD cuando cambian las existencias
 * de una Bodega_Tienda.
 * <p>
 * Separado del listener del traspaso automático. A diferencia de aquel, reacciona
 * también a {@code TRASPASO_SALIDA} (el traspaso baja la Bodega) y a los eventos sin
 * movimiento, en los que solo cambia {@code entrante} o {@code saliente} y
 * {@code tipoMovimiento} es nulo .
 * <p>
 * Corre AFTER_COMMIT en su propia transacción y en el tercer lugar: después de
 * ventas (10) y del traspaso (20). Si falla, se registra y no revierte el origen.
 */
@Component
public class RecomendacionExistenciasListener {

    /** ventas 10, traspaso 20, recomendación 30. */
    public static final int ORDEN = 30;

    private static final Logger log = LoggerFactory.getLogger(RecomendacionExistenciasListener.class);

    private final EvaluarRecomendacionUseCase evaluarRecomendacion;

    public RecomendacionExistenciasListener(EvaluarRecomendacionUseCase evaluarRecomendacion) {
        this.evaluarRecomendacion = evaluarRecomendacion;
    }

    @Order(ORDEN)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExistenciasActualizadas(ExistenciasActualizadas evento) {
        if (evento.tipoNodo() != TipoNodoInventario.BODEGA_TIENDA || evento.tiendaId() == null) {
            return;
        }
        try {
            evaluarRecomendacion.evaluar(evento.tiendaId(), evento.varianteId());
        } catch (RuntimeException e) {
            log.warn("No se pudo evaluar la recomendación de la variante {} en la tienda {}",
                    evento.varianteId(), evento.tiendaId(), e);
        }
    }
}
