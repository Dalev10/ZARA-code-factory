package com.codefactory.supplychain.inventario.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consulta pública del historial de movimientos del kardex.
 *
 * <h2>Cómo se consume</h2>
 * Pensado para reconciliaciones (por ejemplo, fechas de ventanas de quiebre,
 * D-49). El consumidor declara un puerto propio en
 * {@code application/port/out/} y un adaptador en
 * {@code infrastructure/adapter/out/inventario/} traduce
 * {@link MovimientoKardex} a sus tipos:
 *
 * <pre>{@code
 * // ventas/infrastructure/adapter/out/inventario/HistorialStockAdapter.java
 * @Component
 * @RequiredArgsConstructor
 * class HistorialStockAdapter implements HistorialStockPort {
 *     private final ConsultaKardex consultaKardex;
 *
 *     public List<CambioStock> cambios(UUID almacenId, UUID varianteId, Instant desde, Instant hasta) {
 *         return consultaKardex.consultarMovimientos(almacenId, varianteId, desde, hasta).stream()
 *                 .map(m -> new CambioStock(m.saldoResultante(), m.fecha()))
 *                 .toList();
 *     }
 * }
 * }</pre>
 *
 * <h2>Cómo se implementa</h2>
 * Dentro del módulo inventario, con un adaptador de solo lectura sobre
 * {@code movimiento_inventario}. {@code cantidad} es el delta aplicado
 * (negativo en salidas) y {@code saldoResultante} el {@code aLaMano} después
 * del movimiento.
 */
public interface ConsultaKardex {

    /**
     * Consulta los movimientos de una variante en un nodo dentro de un rango.
     *
     * TODO: confirmar con Altamiranda y Cerquera si el rango
     * debe ser inclusivo y qué ordenamiento necesitan.
     */
    List<MovimientoKardex> consultarMovimientos(
            UUID nodoId,
            UUID varianteId,
            Instant desde,
            Instant hasta);
}
