package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.InventarioJpaEntity;

/**
 * Repositorio JPA TEMPORAL para la proyección de Inventario usada por
 * BodegaTienda.
 */
public interface InventarioJpaRepository extends JpaRepository<InventarioJpaEntity, Long> {

    List<InventarioJpaEntity> findByNodoId(Long nodoId);
}
