package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consulta pública del historial de movimientos del kardex.
 */
public interface ConsultaKardex {

    /**
     * Consulta los movimientos de una variante en un nodo dentro de un rango.
     *
     * TODO: confirmar con Altamiranda y Cerquera si el rango
     * debe ser inclusivo y qué ordenamiento necesitan.
     */
    List<MovimientoKardex> consultarMovimientos(
            UUID nodoId,
            UUID varianteId,
            Instant desde,
            Instant hasta);
}
