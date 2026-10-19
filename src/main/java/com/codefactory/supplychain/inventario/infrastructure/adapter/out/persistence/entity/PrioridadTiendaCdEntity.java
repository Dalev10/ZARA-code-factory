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

import java.util.UUID;

/**
 * Entidad JPA de {@code tienda_cd_prioridad} (V1, ajustada en V23).
 * Las FK a tienda y cd son UUID simples: validadas por la BD, no como relaciones JPA.
 *
 * Módulo: inventario — HU-42.
 */
@Entity
@Table(name = "tienda_cd_prioridad")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PrioridadTiendaCdEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(name = "cd_id", nullable = false)
    private UUID cdId;

    @Column(name = "orden_prioridad", nullable = false)
    private int ordenPrioridad;
}
