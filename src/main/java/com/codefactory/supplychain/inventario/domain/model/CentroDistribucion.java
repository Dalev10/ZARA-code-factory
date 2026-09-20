package com.codefactory.supplychain.inventario.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CentroDistribucion {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String nombre;
    private final String ubicacion;

    public CentroDistribucion(UUID id, String nombre, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }

    public static CentroDistribucion crear(String nombre, String ubicacion) {
        return new CentroDistribucion(UUID.randomUUID(), nombre, ubicacion);
    }
}
