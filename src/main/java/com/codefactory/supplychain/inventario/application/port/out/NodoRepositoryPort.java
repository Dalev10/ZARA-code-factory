package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface NodoRepositoryPort {

    Nodo guardar(Nodo nodo);

    Optional<Nodo> buscarPorId(UUID id);

    Optional<Nodo> buscarPorCdId(UUID cdId);

    Optional<Nodo> buscarPorTiendaYTipo(UUID tiendaId, TipoNodo tipo);

    Page<Nodo> listarPorTipo(TipoNodo tipo, Pageable pageable);

    boolean existePorTiendaYTipo(UUID tiendaId, TipoNodo tipo);

    void eliminar(UUID id);
}
