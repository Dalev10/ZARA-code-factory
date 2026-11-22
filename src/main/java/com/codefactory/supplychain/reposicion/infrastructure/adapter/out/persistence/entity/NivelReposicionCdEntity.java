package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad JPA de la tabla {@code nivel_reposicion_cd} (V35). Variante y categoría son UUID simples:
 * catalogo es otro módulo. La integridad la da la FK de la base.
 */
@Entity
@Table(name = "nivel_reposicion_cd")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NivelReposicionCdEntity {

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
