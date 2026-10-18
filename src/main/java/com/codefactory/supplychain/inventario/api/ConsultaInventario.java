package com.codefactory.supplychain.inventario.api;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Consulta pública de existencias para otros módulos.
 *
 * <h2>Cómo se consume</h2>
 * El consumidor declara un puerto propio en {@code application/port/out/} y lo
 * implementa con un adaptador en {@code infrastructure/adapter/out/inventario/},
 * único archivo que importa este paquete. El adaptador traduce
 * {@link StockResumen} a los tipos del consumidor:
 *
 * <pre>{@code
 * // ventas/infrastructure/adapter/out/inventario/StockAlmacenAdapter.java
 * @Component
 * @RequiredArgsConstructor
 * class StockAlmacenAdapter implements StockAlmacenPort {
 *     private final ConsultaInventario consultaInventario;
 *
 *     public Optional<Integer> aLaMano(UUID almacenId, UUID varianteId) {
 *         return consultaInventario.consultarStock(almacenId, varianteId)
 *                 .map(StockResumen::aLaMano);
 *     }
 * }
 * }</pre>
 *
 * <h2>Cómo se implementa</h2>
 * Dentro del módulo inventario, con un adaptador que lee mediante los puertos
 * de persistencia y proyecta a {@link StockResumen}. Es de solo lectura: no
 * modifica filas, no registra kardex ni publica eventos.
 */
public interface ConsultaInventario {

    /**
     * Consulta las magnitudes de una variante en un nodo.
     *
     * Devuelve {@code Optional.empty()} si la variante no tiene carga inicial en
     * el nodo; no se interpreta como stock cero.
     *
     * TODO: confirmar con Altamiranda y Cerquera las
     * magnitudes, filtros y comportamiento cuando no exista inventario.
     */
    Optional<StockResumen> consultarStock(UUID nodoId, UUID varianteId);

    /**
     * Consulta las existencias de todas las variantes almacenadas en un nodo.
     * Una lista vacía significa que el nodo no tiene cargas.
     *
     * TODO: confirmar con Altamiranda y Cerquera si deben
     * incluirse variantes con saldo cero.
     */
    List<StockResumen> consultarExistenciasPorNodo(UUID nodoId);
}
