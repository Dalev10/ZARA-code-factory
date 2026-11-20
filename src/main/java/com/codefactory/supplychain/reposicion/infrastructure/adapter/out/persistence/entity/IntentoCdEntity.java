package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.reposicion.domain.model.ResultadoIntentoCd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code intento_cd}. La FK a la orden la
 * maneja {@link OrdenReabastecimientoEntity}.
 *
 * Módulo: reposicion.
 */
@Entity
@Table(name = "intento_cd")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IntentoCdEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "cd_id", nullable = false)
    private UUID cdId;

    @Column(name = "orden_evaluado", nullable = false)
    private int ordenEvaluado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoIntentoCd resultado;

    @Column(length = 255)
    private String motivo;

    @Column(name = "timestamp_intento", nullable = false)
    private Instant timestampIntento;
}
