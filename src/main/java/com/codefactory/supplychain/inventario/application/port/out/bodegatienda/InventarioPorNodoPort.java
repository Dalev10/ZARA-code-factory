package com.codefactory.supplychain.inventario.application.port.out.bodegatienda;

import java.util.List;

/**
 * Puerto TEMPORAL para consultar el inventario asociado a un nodo en las
 * lecturas de BodegaTienda.
 */
public interface InventarioPorNodoPort {

    List<InventarioResumen> consultarPorNodo(Long nodoId);

    /**
     * DTO TEMPORAL con el resumen de inventario requerido por HU-08.
     */
    record InventarioResumen(Long varianteId, Integer aLaMano, Integer disponibleParaUso) {
    }
}
