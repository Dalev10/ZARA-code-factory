package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface TiendaUseCase {

    Tienda crear(String nombre, String ubicacion);

    Tienda obtenerPorId(UUID id);

    Page<Tienda> listar(Pageable pageable);

    Tienda actualizar(UUID id, String nombre, String ubicacion);

    void desactivar(UUID id);

    Tienda activar(UUID id);

    /** Id del nodo ALMACEN de la tienda; vacío si no lo tiene. */
    Optional<UUID> buscarAlmacen(UUID tiendaId);

    /** Id del nodo BODEGA_TIENDA de la tienda; vacío si no lo tiene. */
    Optional<UUID> buscarBodega(UUID tiendaId);
}
