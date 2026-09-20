package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Entity
@Table(name = "cd")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CentroDistribucionEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "ubicacion")
    private String ubicacion;
}
