package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code recomendacion} (V1, ajustada en V32 y V37).
 * Las columnas de aprobación ({@code cantidad_aprobada}, {@code aprobado_por},
 * {@code fecha_aprobacion}).
 *
 * Módulo: reposicion.
 */
@Entity
@Table(name = "recomendacion")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecomendacionEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(name = "cd_sugerido_id")
    private UUID cdSugeridoId;

    @Column(name = "cantidad_sugerida", nullable = false)
    private int cantidadSugerida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecomendacion estado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "cantidad_aprobada")
    private Integer cantidadAprobada;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private Instant fechaAprobacion;

    @Version
    private Long version;
}
