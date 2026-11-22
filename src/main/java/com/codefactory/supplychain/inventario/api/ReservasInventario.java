package com.codefactory.supplychain.inventario.api;

import java.util.UUID;

/**
 * Reserva de existencias de un CD para abastecer la Bodega_Tienda de una tienda
 * (D-81). Consumidor: reposicion (HU-45 crea la orden; HU-53 la cierra o cancela).
 *
 * <h2>Semántica común</h2>
 * <ul>
 *   <li>Inventario resuelve los nodos: el del CD a partir de {@code cdId} y la
 *       Bodega_Tienda a partir de {@code tiendaId}.</li>
 *   <li>Las filas se acceden siempre en el orden CD → Bodega_Tienda (D-106).</li>
 *   <li>Reservar y liberar solo cambian {@code saliente} y {@code entrante}: no
 *       registran movimiento en el kardex (D-93). Sí publican
 *       {@link ExistenciasActualizadas}, con {@code movimientoId} y
 *       {@code tipoMovimiento} nulos (D-100).</li>
 *   <li>Transacción: la operación se une a la del llamador y no tiene reintento
 *       propio. El reintento ante conflicto de versión va en el punto de entrada
 *       del llamador, fuera de su transacción (D-24).</li>
 * </ul>
 */
public interface ReservasInventario {

    /**
     * Reserva {@code cantidad} unidades en el CD para la tienda.
     *
     * Si el disponible del CD ({@code a_la_mano − saliente}) cubre la cantidad,
     * suma {@code saliente} en el CD y {@code entrante} en la Bodega_Tienda,
     * juntos, y responde {@link EstadoReserva#ASIGNADO}. Si no la cubre, no
     * cambia nada y responde {@link EstadoReserva#SIN_DISPONIBLE} con el
     * disponible actual: es flujo normal del fallback de CD, no un error.
     *
     * @param referenciaId id de la orden de reabastecimiento
     */
    ResultadoReserva reservar(UUID cdId, UUID tiendaId, UUID varianteId, int cantidad, UUID referenciaId);

    /**
     * Libera lo que quede de una reserva: resta {@code reservaALiberar} del
     * {@code saliente} del CD y {@code entranteALiberar} del {@code entrante}
     * de la Bodega_Tienda. Las dos cantidades son mayores o iguales a cero y
     * nunca dejan valores negativos.
     *
     * @param referenciaId id de la orden de reabastecimiento
     */
    void liberar(
            UUID cdId,
            UUID tiendaId,
            UUID varianteId,
            int reservaALiberar,
            int entranteALiberar,
            UUID referenciaId);
}
