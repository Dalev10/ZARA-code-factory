package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.TiendaJpaEntity;

/**
 * Repositorio provisional de solo consulta para validar tiendas desde
 * {@code BodegaTienda}; no es el repositorio oficial de Tienda.
 */
/**
 * Repositorio JPA TEMPORAL para la proyección de Tienda usada por BodegaTienda.
 */
public interface TiendaJpaRepository extends JpaRepository<TiendaJpaEntity, Long> {
}
