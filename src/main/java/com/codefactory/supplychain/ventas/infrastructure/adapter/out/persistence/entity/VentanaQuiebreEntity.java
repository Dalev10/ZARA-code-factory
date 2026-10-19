package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.ventas.domain.model.MotivoCierreVentana;
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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Mapea {@code ventana_quiebre} (V1 + V20). */
@Entity
@Table(name = "ventana_quiebre")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VentanaQuiebreEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cierre", length = 30)
    private MotivoCierreVentana motivoCierre;
}
