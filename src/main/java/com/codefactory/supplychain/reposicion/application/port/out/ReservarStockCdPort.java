package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.UUID;

/**
 * Reserva en un CD la cantidad de una orden para la Bodega de la tienda, y la libera al cerrarla (HU-53).
 * Participa en la transacción del llamador; no reintenta.
 *
 * Módulo: reposicion.
 */
public interface ReservarStockCdPort {

    /**
     * Si el disponible del CD cubre toda la cantidad, la reserva ({@code saliente} en el
     * CD y {@code entrante} en la Bodega) y responde asignada. Si no, no cambia nada.
     */
    Reserva reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID ordenId);

    /** Devuelve lo que quede de la reserva: {@code saliente} en el CD y {@code entrante} en la Bodega (HU-53). */
    void liberar(UUID cdId, UUID tiendaId, UUID varianteId, int reservaCd, int entranteBodega, UUID ordenId);

    record Reserva(boolean asignada, int disponible) {
    }
}
