package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.Inventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto de persistencia del agregado Inventario.
 */
public interface InventarioRepositoryPort {

    Optional<Inventario> buscarPorNodoYVariante(UUID nodoId, UUID varianteId);

    Inventario guardar(Inventario inventario);

    /**
     * De las variantes dadas, devuelve las que ya tienen fila en el nodo.
     */
    Set<UUID> buscarVariantesCargadas(UUID nodoId, Set<UUID> varianteIds);

    /**
     * Filas de inventario de un nodo, paginadas y ordenadas por variante.
     */
    Page<Inventario> buscarPorNodo(UUID nodoId, Pageable pageable);

    /**
     * Filas de inventario de una variante en todos los nodos.
     */
    List<Inventario> buscarPorVariante(UUID varianteId);

    /**
     * Filas de inventario de una variante solo en los nodos de las tiendas dadas.
     */
    List<Inventario> buscarPorVarianteEnTiendas(UUID varianteId, Set<UUID> tiendaIds);

    /**
     * Filas de inventario de una variante en los centros de distribución.
     */
    List<Inventario> buscarPorVarianteEnCds(UUID varianteId);
}
