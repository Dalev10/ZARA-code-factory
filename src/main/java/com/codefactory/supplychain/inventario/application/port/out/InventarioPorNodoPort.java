package com.codefactory.supplychain.inventario.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Puerto TEMPORAL para consultar el inventario asociado a un nodo en las
 * lecturas de BodegaTienda. La captura y gestión real de Inventario
 * pertenece al Sprint 2.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface InventarioPorNodoPort {

    List<InventarioResumen> consultarPorNodo(UUID nodoId);

    record InventarioResumen(UUID varianteId, Integer aLaMano, Integer disponibleParaUso) {
    }
}
