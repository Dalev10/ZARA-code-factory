package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.Tienda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
}
