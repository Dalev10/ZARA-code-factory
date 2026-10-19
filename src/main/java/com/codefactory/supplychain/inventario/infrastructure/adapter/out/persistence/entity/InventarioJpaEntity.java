package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entidad JPA del agregado Inventario sobre la tabla {@code inventario}.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
@Entity
@Table(name = "inventario")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioJpaEntity {

    @Id
    private UUID id;

    @Column(name = "nodo_id", nullable = false)
    private UUID nodoId;

    @Column(name = "variante_id", nullable = false)
    private UUID varianteId;

    @Column(name = "a_la_mano", nullable = false)
    private int aLaMano;

    @Column(name = "disponible_para_uso", nullable = false)
    private int disponibleParaUso;

    @Column(name = "entrante", nullable = false)
    private int entrante;

    @Column(name = "saliente", nullable = false)
    private int saliente;

    @Column(name = "pronosticado", nullable = false)
    private int pronosticado;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "fecha_actualizacion", nullable = false)
    private java.time.Instant fechaActualizacion;
}
