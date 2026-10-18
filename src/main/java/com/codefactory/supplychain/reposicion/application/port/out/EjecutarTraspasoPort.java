package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida hacia el módulo inventario: aplica el movimiento contable del
 * traspaso (descuenta de Bodega, aumenta en Almacén) de forma atómica.
 * <p>
 * El módulo reposicion no manipula directamente las filas de inventario; delega el
 * efecto al módulo inventario a través de este contrato.
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface EjecutarTraspasoPort {

    /**
     * Mueve {@code cantidad} unidades de la Bodega_Tienda al Almacén de la misma tienda.
     * La operación es atómica: nunca deja la Bodega en negativo (lanza si no hay stock
     * suficiente después de recalcular, aunque esto no debería ocurrir con el cálculo D-54).
     *
     * @param nodoOrigenId  nodo origen del traspaso
     * @param nodoDestinoId nodo destino del traspaso
     * @param varianteId variante que se mueve
     * @param cantidad  unidades a traspasar (> 0)
     * @param traspasoId  ID del traspaso
     */
    void traspasar(UUID tiendaId, UUID varianteId, int cantidad, UUID traspasoId);
}
