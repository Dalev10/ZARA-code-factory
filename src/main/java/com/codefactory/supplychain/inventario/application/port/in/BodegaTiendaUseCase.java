package com.codefactory.supplychain.inventario.application.port.in;

import com.codefactory.supplychain.inventario.application.dto.BodegaTiendaConsulta;
import com.codefactory.supplychain.inventario.domain.model.Nodo;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface BodegaTiendaUseCase {

    Nodo registrar(UUID tiendaId);

    BodegaTiendaConsulta consultarPorId(UUID id);

    BodegaTiendaConsulta consultarPorTiendaId(UUID tiendaId);

    List<Nodo> listarTodas();

    Nodo modificar(UUID id, UUID nuevaTiendaId);

    void eliminar(UUID id);
}
