package com.codefactory.supplychain.inventario.application.port.in;

import java.util.UUID;

/**
 * Reserva y liberación de existencias de un CD para la Bodega_Tienda de una
 * tienda (HD-01, D-81).
 *
 * Se une a la transacción del llamador y no reintenta: el reintento ante un
 * conflicto de versión va en el punto de entrada del llamador (D-24).
 */
public interface ReservarInventarioUseCase {

    /**
     * Si el disponible del CD cubre {@code cantidad}, suma {@code saliente} en el
     * CD y {@code entrante} en la Bodega_Tienda. Si no, no cambia nada.
     */
    ResultadoReservaCd reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId);

    /** Resta {@code saliente} en el CD y {@code entrante} en la Bodega_Tienda, sin bajar de cero. */
    void liberar(UUID cdId, UUID tiendaId, UUID varianteId, int reservaALiberar, int entranteALiberar,
                 UUID referenciaId);
}
