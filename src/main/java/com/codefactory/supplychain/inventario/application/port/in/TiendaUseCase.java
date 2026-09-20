package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.domain.model.Tienda;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface TiendaUseCase {

    Tienda crear(String nombre, String ubicacion);

    Tienda obtenerPorId(UUID id);

    List<Tienda> listar();

    Tienda actualizar(UUID id, String nombre, String ubicacion);

    void desactivar(UUID id);
}
