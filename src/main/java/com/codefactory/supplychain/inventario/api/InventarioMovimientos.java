package com.codefactory.supplychain.inventario.api;

import java.util.UUID;

/**
 * Operaciones de inventario solicitadas por otros módulos.
 *
 * <h2>Cómo se consume</h2>
 * El consumidor (ventas, reposicion) no inyecta esta interfaz en sus casos de
 * uso. Declara un puerto propio en {@code application/port/out/} con sus
 * propios tipos, y un adaptador en
 * {@code infrastructure/adapter/out/inventario/} es el único archivo que
 * importa este paquete:
 *
 * <pre>{@code
 * // ventas/application/port/out/DescontarStockPort.java
 * public interface DescontarStockPort {
 *     void descontar(UUID almacenId, UUID varianteId, int cantidad, UUID ventaId, boolean forzado);
 * }
 *
 * // ventas/infrastructure/adapter/out/inventario/DescontarStockAdapter.java
 * @Component
 * @RequiredArgsConstructor
 * class DescontarStockAdapter implements DescontarStockPort {
 *     private final InventarioMovimientos inventarioMovimientos;
 *
 *     public void descontar(UUID almacenId, UUID varianteId, int cantidad, UUID ventaId, boolean forzado) {
 *         inventarioMovimientos.descontarPorVenta(almacenId, varianteId, cantidad, ventaId, forzado);
 *     }
 * }
 * }</pre>
 *
 * <h2>Cómo se implementa</h2>
 * La implementación vive dentro del módulo inventario, en un adaptador de
 * entrada que traduce y delega en {@code InventarioUseCase} (punto de entrada
 * con reintento optimista, D-24). No contiene reglas de negocio: esas viven en
 * el agregado {@code Inventario}.
 *
 * <h2>Semántica común</h2>
 * <ul>
 *   <li>Es síncrono (D-15): el consumidor necesita saber si el movimiento se
 *       aplicó.</li>
 *   <li>Opera solo sobre filas cargadas: una variante sin carga inicial en el
 *       nodo se rechaza siempre, incluso con {@code forzado = true}.</li>
 *   <li>Cada operación registra un movimiento en el kardex y publica
 *       {@link ExistenciasActualizadas}.</li>
 *   <li>Transacción: si el llamador ya tiene una abierta, la operación se une
 *       a ella y el movimiento se revierte junto con el llamador. En ese caso
 *       el reintento optimista de inventario no puede actuar, porque el
 *       conflicto de versión aparece al confirmar la transacción del
 *       llamador; el reintento debe ir en el punto de entrada del llamador,
 *       fuera de su transacción (AGENTS.md §7).</li>
 * </ul>
 */
public interface InventarioMovimientos {

    /**
     * Descuenta existencias por una venta confirmada.
     *
     * Sin {@code forzado}, lanza {@link StockInsuficienteException} si
     * {@code cantidad} supera el {@code aLaMano}. Con {@code forzado}, el
     * descuento se aplica aunque el saldo quede negativo y el movimiento queda
     * marcado como forzado (D-35).
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
     * Revierte el descuento producido por una venta anulada. Devuelve
     * {@code cantidad} unidades al nodo con un movimiento ANULACION_VENTA.
     *
     * TODO: confirmar con Altamiranda y Cerquera si esta
     * operación debe tener un contrato o evento adicional.
     */
    void revertirPorAnulacion(UUID nodoId, UUID varianteId, int cantidad, UUID ventaId);

    /**
     * Traslada existencias entre dos nodos: TRASPASO_SALIDA en el origen y
     * TRASPASO_ENTRADA en el destino, en la misma transacción y bloqueando
     * Bodega antes que Almacén (D-24).
     *
     * TODO: confirmar con Cerquera los datos del
     * traspaso, sus reglas de idempotencia y la semántica de cantidades.
     */
    void trasladar(UUID nodoOrigenId, UUID nodoDestinoId, UUID varianteId, int cantidad, UUID traspasoId);
}
