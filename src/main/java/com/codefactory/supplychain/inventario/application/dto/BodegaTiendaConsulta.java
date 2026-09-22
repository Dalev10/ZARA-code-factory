package com.codefactory.supplychain.inventario.application.dto;

import com.codefactory.supplychain.inventario.application.port.out.InventarioPorNodoPort.InventarioResumen;
import com.codefactory.supplychain.inventario.domain.model.Nodo;
import com.codefactory.supplychain.inventario.domain.model.Tienda;

import java.util.List;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record BodegaTiendaConsulta(Nodo nodo, Tienda tienda, List<InventarioResumen> inventario) {
}
