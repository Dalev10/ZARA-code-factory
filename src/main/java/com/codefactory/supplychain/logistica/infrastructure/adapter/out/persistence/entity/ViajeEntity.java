package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA de {@code viaje} (V1, ajustada en V39) con sus paradas. Bloqueo optimista con
 * {@code @Version}: cada cambio de una parada pasa por el viaje.
 *
 * Módulo: logistica.
 */
@Entity
@Table(name = "viaje")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ViajeEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "cd_id", nullable = false)
    private UUID cdId;

    @Column(length = 100)
    private String vehiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoViaje estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_salida")
    private Instant fechaSalida;

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @Version
    private Long version;

    /** Lote de 100: un listado paginado carga las paradas de toda la página en una consulta. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "viaje_id", nullable = false)
    @OrderBy("ordenParada ASC")
    @BatchSize(size = 100)
    private List<ViajeParadaEntity> paradas = new ArrayList<>();
}
