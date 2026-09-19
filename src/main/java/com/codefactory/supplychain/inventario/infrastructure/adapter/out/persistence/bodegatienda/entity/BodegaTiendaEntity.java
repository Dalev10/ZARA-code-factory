package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity;

import com.codefactory.supplychain.inventario.domain.model.TipoNodo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para una bodega de tienda almacenada en {@code nodo}.
 */
@Entity
@Table(name = "nodo")
public class BodegaTiendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoNodo tipo;

    @Column(name = "tienda_id", nullable = false)
    private Long tiendaId;

    protected BodegaTiendaEntity() {
    }

    public BodegaTiendaEntity(Long id, Long tiendaId) {
        this.id = id;
        this.tiendaId = tiendaId;
        this.tipo = TipoNodo.BODEGA_TIENDA;
    }

    public Long getId() {
        return id;
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    public Long getTiendaId() {
        return tiendaId;
    }
}
