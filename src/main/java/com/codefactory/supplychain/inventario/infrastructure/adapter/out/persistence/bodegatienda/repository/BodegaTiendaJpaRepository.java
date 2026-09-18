package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.bodegatienda.entity.BodegaTiendaEntity;

/**
 * Repositorio Spring Data para consultar y persistir bodegas de tienda.
 */
public interface BodegaTiendaJpaRepository extends JpaRepository<BodegaTiendaEntity, Long> {

    List<BodegaTiendaEntity> findByTipo(TipoNodo tipo);

    Optional<BodegaTiendaEntity> findByIdAndTipo(Long id, TipoNodo tipo);

    Optional<BodegaTiendaEntity> findByTiendaIdAndTipo(Long tiendaId, TipoNodo tipo);

    boolean existsByIdAndTipo(Long id, TipoNodo tipo);

    boolean existsByTiendaIdAndTipo(Long tiendaId, TipoNodo tipo);

    long deleteByIdAndTipo(Long id, TipoNodo tipo);
}
