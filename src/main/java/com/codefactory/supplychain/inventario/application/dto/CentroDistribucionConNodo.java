package com.codefactory.supplychain.inventario.application.dto;

import com.codefactory.supplychain.inventario.domain.model.CentroDistribucion;
import com.codefactory.supplychain.inventario.domain.model.Nodo;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record CentroDistribucionConNodo(CentroDistribucion centroDistribucion, Nodo nodo) {
}
