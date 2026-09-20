package com.codefactory.supplychain.inventario.infrastructure.adapter.in.web.dto;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public record CentroDistribucionResponse(UUID id, String nombre, String ubicacion) {
}
