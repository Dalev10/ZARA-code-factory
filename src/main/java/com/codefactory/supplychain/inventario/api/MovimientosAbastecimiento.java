package com.codefactory.supplychain.inventario.api;

import java.util.UUID;

/**
 * Movimientos físicos del abastecimiento CD → Bodega_Tienda. Consumidor:
 * logistica (HU-46 despacha, HU-49 recibe).
 *
 * <h2>Semántica común</h2>
 * <ul>
 *   <li>Cada operación cambia {@code a_la_mano}, registra su movimiento en el
 *       kardex ({@code DESPACHO} o {@code RECEPCION}, con {@code referenciaId})
 *       y publica {@link ExistenciasActualizadas}.</li>
 *   <li>Transacción: la operación se une a la del llamador y no tiene reintento
 *       propio. El reintento ante conflicto de versión va en el punto de entrada
 *       del llamador, fuera de su transacción (D-24).</li>
 * </ul>
 */
public interface MovimientosAbastecimiento {

    /**
     * Registra la salida física de un despacho en el CD:
     * {@code a_la_mano −= cantidadQueSale} y {@code saliente −= reservaALiberar}
     * (sin bajar de cero).
     *
     * Son dos cantidades porque no siempre coinciden: al despachar de nuevo
     * unidades perdidas tras una reapertura, sale mercancía pero la reserva ya
     * se consumió en el primer despacho (D-92). Lanza
     * {@link StockInsuficienteException} si {@code a_la_mano < cantidadQueSale}.
     *
     * @param referenciaId id del ciclo de despacho
     */
    void registrarDespacho(UUID cdId, UUID varianteId, int cantidadQueSale, int reservaALiberar, UUID referenciaId);

    /**
     * Registra lo recibido en la Bodega_Tienda de la tienda:
     * {@code a_la_mano += cantidad} y {@code entrante −= cantidad} (sin bajar de
     * cero). Si la variante nunca estuvo en la Bodega_Tienda, crea la fila
     * (D-104). No lanza errores de stock.
     *
     * @param referenciaId id de la recepción
     */
    void registrarRecepcion(UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId);
}
