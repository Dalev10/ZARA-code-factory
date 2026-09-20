package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad JPA TEMPORAL de solo lectura sobre la tabla {@code inventario}.
 * No representa la entidad oficial del módulo Inventario (Sprint 2).
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Entity
@Table(name = "inventario")
@Getter
@NoArgsConstructor
public class InventarioJpaEntity {

    @Id
    private UUID id;

    @Column(name = "nodo_id")
    private UUID nodoId;

    @Column(name = "variante_id")
    private UUID varianteId;

    @Column(name = "a_la_mano")
    private Integer aLaMano;

    @Column(name = "disponible_para_uso")
    private Integer disponibleParaUso;
}
