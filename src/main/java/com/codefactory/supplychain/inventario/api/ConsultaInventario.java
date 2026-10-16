package com.codefactory.supplychain.inventario.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta pública de existencias para otros módulos.
 */
public interface ConsultaInventario {

    /**
     * Consulta las magnitudes de una variante en un nodo.
     *
     * TODO: confirmar con Altamiranda y Cerquera las
     * magnitudes, filtros y comportamiento cuando no exista inventario.
     */
    Optional<StockResumen> consultarStock(UUID nodoId, UUID varianteId);
}
