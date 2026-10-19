package com.codefactory.supplychain.inventario.application.port.in;

import java.util.UUID;

/**
 * Resumen de una carga inicial aplicada.
 */
public record ResultadoCargaInicial(UUID nodoId, int filasCreadas, int unidadesTotales) {
}
