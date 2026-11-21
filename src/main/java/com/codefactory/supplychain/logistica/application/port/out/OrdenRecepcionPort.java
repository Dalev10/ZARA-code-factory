package com.codefactory.supplychain.logistica.application.port.out;

import com.codefactory.supplychain.logistica.domain.exception.OrdenNoAdmiteRecepcionException;

import java.util.UUID;

/**
 * La orden de reabastecimiento que abastece una parada, vista desde la recepción (HU-49).
 * Se une a la transacción del llamador.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface OrdenRecepcionPort {

    /** Variante y situación actual de la orden. */
    Orden consultar(UUID ordenId);

    /**
     * Registra lo recibido del ciclo en la orden: queda CERRADA si no hay pendiente o
     * REABIERTA si lo hay.
     *
     * @throws OrdenNoAdmiteRecepcionException si la orden no está EN_TRANSITO
     */
    Situacion registrarRecepcion(UUID ordenId, int cantidadRecibida);

    record Orden(UUID varianteId, Situacion situacion) {
    }

    /**
     * @param estado            nombre del estado de la orden, p. ej. {@code "CERRADA"}
     * @param cantidadPendiente solicitado − recibido acumulado
     */
    record Situacion(String estado, int cantidadPendiente) {
    }
}
