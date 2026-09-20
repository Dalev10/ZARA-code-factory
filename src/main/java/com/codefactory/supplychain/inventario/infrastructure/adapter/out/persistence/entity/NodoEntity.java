package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity;

import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
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

import java.util.UUID;

/**
 * Única entidad JPA mapeada a {@code nodo} — CD, BodegaTienda y Almacén
 * comparten esta tabla y este mapeo; no cada feature con la suya.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Entity
@Table(name = "nodo")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NodoEntity {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoNodo tipo;

    @Column(name = "cd_id")
    private UUID cdId;

    @Column(name = "tienda_id")
    private UUID tiendaId;
}
