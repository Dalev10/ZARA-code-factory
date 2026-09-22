package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.InventarioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA TEMPORAL para la proyección de Inventario usada por
 * BodegaTienda.
 *
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface InventarioJpaRepository extends JpaRepository<InventarioJpaEntity, UUID> {

    List<InventarioJpaEntity> findByNodoId(UUID nodoId);
}
