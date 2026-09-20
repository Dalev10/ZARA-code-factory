package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * Entidad JPA TEMPORAL de solo lectura sobre la tabla {@code inventario}.
 *
 * No representa la entidad oficial del módulo Inventario.
 */
@Entity
@Table(name = "inventario")
public class InventarioJpaEntity {

    @Id
    private Long id;

    @Column(name = "nodo_id")
    private Long nodoId;
    @Column(name = "variante_id")
    private Long varianteId;
    @Column(name = "a_la_mano")
    private Integer aLaMano;
    @Column(name = "disponible_para_uso")
    private Integer disponibleParaUso;

    protected InventarioJpaEntity() {
    }

    public Long getNodoId() {
        return nodoId;
    }

    public Long getVarianteId() {
        return varianteId;
    }

    public Integer getALaMano() {
        return aLaMano;
    }

    public Integer getDisponibleParaUso() {
        return disponibleParaUso;
    }
}
