package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.TiendaExistenciaJpaEntity;

/**
 * Repositorio provisional de solo consulta para validar tiendas desde
 * {@code BodegaTienda}; no es el repositorio oficial de Tienda.
 */
public interface TiendaExistenciaJpaRepository extends JpaRepository<TiendaExistenciaJpaEntity, Long> {
}
