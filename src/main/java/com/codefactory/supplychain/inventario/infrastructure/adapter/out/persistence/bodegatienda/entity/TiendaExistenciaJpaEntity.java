package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Proyección JPA provisional de {@code tienda}, exclusiva para consultar si
 * existe una tienda asociada a una bodega.
 *
 * No representa la entidad oficial de Tienda.
 */
@Entity
@Table(name = "tienda")
public class TiendaExistenciaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected TiendaExistenciaJpaEntity() {
    }

    public Long getId() {
        return id;
    }
}
