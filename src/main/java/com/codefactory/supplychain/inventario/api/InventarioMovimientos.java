package com.codefactory.supplychain.inventario.api;

import java.util.UUID;

/**
 * Operaciones de inventario solicitadas por otros módulos.
 *
 * Este contrato no contiene implementación: el módulo inventario lo
 * implementará cuando se construya el agregado Inventario.
 */
public interface InventarioMovimientos {

    /**
     * Descuenta existencias por una venta confirmada.
     *
     * TODO: confirmar con Altamiranda los identificadores, cantidad,
     * semántica de idempotencia y quién puede solicitar una venta forzada.
     */
    void descontarPorVenta(
            UUID nodoId,
            UUID varianteId,
            int cantidad,
            UUID ventaId,
            boolean forzado);

    /**
     * Revierte el descuento producido por una venta anulada.
     *
     * TODO: confirmar con Altamiranda y Cerquera si esta
     * operación debe tener un contrato o evento adicional.
     */
    void revertirPorAnulacion(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId);

    /**
     * Traslada existencias entre dos nodos.
     *
     * TODO: confirmar con Cerquera los datos del
     * traspaso, sus reglas de idempotencia y la semántica de cantidades.
     */
    void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId);
}
