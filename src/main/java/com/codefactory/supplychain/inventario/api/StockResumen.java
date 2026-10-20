package com.codefactory.supplychain.inventario.api;

import java.util.UUID;

/**
 * Resumen estable de las existencias de una variante en un nodo.
 */
public record StockResumen(
        UUID nodoId,
        UUID varianteId,
        int aLaMano,
        int disponibleParaUso,
        int entrante,
        int saliente,
        int pronosticado) {
}
