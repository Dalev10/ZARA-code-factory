package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.TipoNodo;

import java.util.List;
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

    List<Nodo> listarPorTipo(TipoNodo tipo);

    boolean existePorTiendaYTipo(UUID tiendaId, TipoNodo tipo);

    void eliminar(UUID id);
}
