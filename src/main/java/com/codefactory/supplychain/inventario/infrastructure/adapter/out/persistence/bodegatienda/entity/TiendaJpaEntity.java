package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * Proyección JPA TEMPORAL de {@code tienda}, exclusiva para consultar si
 * existe y obtener los datos de una tienda asociada a una bodega.
 *
 * No representa la entidad oficial de Tienda.
 */
@Entity
@Table(name = "tienda")
public class TiendaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "ubicacion")
    private String ubicacion;

    protected TiendaJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }
}
