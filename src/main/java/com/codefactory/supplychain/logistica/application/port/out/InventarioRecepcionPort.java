package com.codefactory.supplychain.logistica.application.port.out;

import java.util.UUID;

/**
 * Suma lo recibido a la Bodega_Tienda (HU-49). Si la variante nunca estuvo en la Bodega_Tienda,
 * crea su fila (D-104). Se une a la transacción del llamador.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface InventarioRecepcionPort {

    /**
     * @param cantidad     mayor que cero
     * @param recepcionId  referencia del movimiento de kardex
     */
    void sumarRecibido(UUID tiendaId, UUID varianteId, int cantidad, UUID recepcionId);
}
