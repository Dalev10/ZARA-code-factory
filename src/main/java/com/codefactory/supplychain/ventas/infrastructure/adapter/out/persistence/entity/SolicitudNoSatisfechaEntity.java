package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.ventas.domain.model.EstadoSolicitud;
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

/** Mapea {@code solicitud_no_satisfecha} (V1 + V19). */
@Entity
@Table(name = "solicitud_no_satisfecha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SolicitudNoSatisfechaEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "tienda_id", nullable = false)
    private UUID tiendaId;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    @Column(name = "stock_sistema")
    private Integer stockSistema;

    @Column(name = "timestamp_solicitud", nullable = false)
    private Instant registradaEn;

    @Column(name = "registrado_por_id")
    private UUID registradoPor;

    @Column(name = "clave_idempotencia", nullable = false, length = 100)
    private String claveIdempotencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "anulada_por")
    private UUID anuladaPor;

    @Column(name = "anulada_en")
    private Instant anuladaEn;
}
