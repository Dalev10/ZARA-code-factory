package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code recepcion} (V1, ajustada en V36). La parada y el ciclo son
 * UUID simples: la integridad la dan las FK de la base.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
@Entity
@Table(name = "recepcion")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecepcionEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "viaje_parada_id", nullable = false, updatable = false)
    private UUID viajeParadaId;

    @Column(name = "ciclo_despacho_id", nullable = false, updatable = false)
    private UUID cicloDespachoId;

    @Column(name = "cantidad_recibida", nullable = false, updatable = false)
    private int cantidadRecibida;

    @Column(name = "discrepancia", nullable = false, updatable = false)
    private int discrepancia;

    @Column(name = "registrado_por", nullable = false, updatable = false)
    private UUID registradoPor;

    @Column(name = "timestamp_recepcion", nullable = false, updatable = false)
    private Instant timestampRecepcion;
}
