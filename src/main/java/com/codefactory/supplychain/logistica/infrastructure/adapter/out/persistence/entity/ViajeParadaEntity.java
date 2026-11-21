package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.logistica.domain.model.EstadoParada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA de {@code viaje_parada} (V1, ajustada en V39). La tienda es un UUID simple: vive en
 * inventario. La escribe solo su {@link ViajeEntity}.
 *
 * Módulo: logistica.
 */
@Entity
@Table(name = "viaje_parada")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ViajeParadaEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "ciclo_despacho_id", nullable = false)
    private UUID cicloDespachoId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(name = "orden_parada", nullable = false)
    private int ordenParada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoParada estado;

    @Column(name = "timestamp_salida_cd")
    private Instant fechaSalidaCd;

    @Column(name = "timestamp_llegada_tienda")
    private Instant fechaLlegada;
}
