package com.codefactory.supplychain.inventario.application.port.out;

import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface TiendaRepositoryPort {

    Tienda guardar(Tienda tienda);

    Optional<Tienda> buscarPorId(UUID id);

    Page<Tienda> listarTodas(Pageable pageable);

    boolean existePorNombre(String nombre);
}
