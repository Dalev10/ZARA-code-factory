package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.Tienda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface TiendaRepositoryPort {

    Tienda guardar(Tienda tienda);

    Optional<Tienda> buscarPorId(UUID id);

    List<Tienda> listarTodas();

    boolean existePorNombre(String nombre);
}
