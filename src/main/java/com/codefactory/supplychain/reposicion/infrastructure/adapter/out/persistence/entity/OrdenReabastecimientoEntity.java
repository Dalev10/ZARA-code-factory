package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
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
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code orden_reabastecimiento}.
 * Bloqueo optimista con {@code @Version}.
 *
 * Módulo: reposicion.
 */
@Entity
@Table(name = "orden_reabastecimiento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrdenReabastecimientoEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "recomendacion_id")
    private UUID recomendacionId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "cd_asignado_id")
    private UUID cdAsignadoId;

    @Column(name = "motivo_asignacion", length = 255)
    private String motivoAsignacion;

    @Column(name = "cantidad_total_solicitada", nullable = false)
    private int cantidadTotalSolicitada;

    @Column(name = "cantidad_pendiente", nullable = false)
    private int cantidadPendiente;

    @Column(name = "cantidad_despachada_acumulada", nullable = false)
    private int cantidadDespachadaAcumulada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoOrden estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "orden_id", nullable = false)
    @OrderBy("ordenEvaluado ASC")
    @Builder.Default
    private List<IntentoCdEntity> intentos = new ArrayList<>();
}
