package com.codefactory.supplychain.reposicion.application.port.out;

import java.util.UUID;

/**
 * Puerto de salida hacia el módulo inventario: aplica el movimiento contable del
 * traspaso (descuenta de la Bodega, aumenta en el Almacén) de forma atómica.
 * <p>
 * El módulo reposicion no manipula directamente las filas de inventario; delega el
 * efecto al módulo inventario a través de este contrato.
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface EjecutarTraspasoPort {

    /**
     * Mueve {@code cantidad} unidades de la Bodega_Tienda al Almacén de la misma tienda.
     *
     * @return {@code false} si la Bodega no tiene esas unidades; nunca la deja en negativo
     * @throws com.codefactory.supplychain.reposicion.domain.exception.ReposicionRecursoNoEncontradoException
     *         si la tienda no tiene Bodega o Almacén
     */
    boolean traspasar(UUID tiendaId, UUID varianteId, int cantidad, UUID traspasoId);
}
