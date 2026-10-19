package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity;

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
 * Entidad JPA que mapea la tabla {@code umbral_reposicion_interna} (V1, ajustada en V21).
 * <p>
 * Las referencias a variante y categoría son UUID simples y no relaciones JPA: catalogo es
 * otro módulo y reposicion no conoce sus entidades. La integridad la da la FK de la base.
 */
@Entity
@Table(name = "umbral_reposicion_interna")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UmbralReposicionEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "variante_id")
    private UUID varianteId;

    @Column(name = "categoria_id")
    private UUID categoriaId;

    @Column(name = "cantidad_minima", nullable = false)
    private int cantidadMinima;

    @Column(name = "cantidad_objetivo", nullable = false)
    private int cantidadObjetivo;
}
