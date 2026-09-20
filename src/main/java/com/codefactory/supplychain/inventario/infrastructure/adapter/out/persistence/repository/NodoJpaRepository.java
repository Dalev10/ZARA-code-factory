package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.domain.model.TipoNodo;
import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.NodoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: inventario — Red de nodos (CD, Tienda/Almacén, Bodega_Tienda) e inventario polimórfico sobre ellos.
 */
public interface NodoJpaRepository extends JpaRepository<NodoEntity, UUID> {

    Optional<NodoEntity> findByCdId(UUID cdId);

    Optional<NodoEntity> findByTiendaIdAndTipo(UUID tiendaId, TipoNodo tipo);

    Page<NodoEntity> findByTipo(TipoNodo tipo, Pageable pageable);

    boolean existsByTiendaIdAndTipo(UUID tiendaId, TipoNodo tipo);
}
