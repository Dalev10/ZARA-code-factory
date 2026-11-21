package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.logistica.domain.model.ResultadoDespacho;
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
 * Entidad JPA de {@code ciclo_despacho} (V1, ajustada en V38). La orden es un UUID simple:
 * vive en reposicion. Sin {@code @Version}: un ciclo no cambia después de crearse.
 *
 * Módulo: logistica.
 */
@Entity
@Table(name = "ciclo_despacho")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CicloDespachoEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "orden_id", nullable = false)
    private UUID ordenId;

    @Column(name = "numero_ciclo", nullable = false)
    private int numeroCiclo;

    @Column(name = "cantidad_solicitada", nullable = false)
    private int cantidadSolicitada;

    @Column(name = "cantidad_despachada", nullable = false)
    private int cantidadDespachada;

    @Column(name = "fecha_despacho", nullable = false)
    private Instant fechaDespacho;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoDespacho resultado;
}
