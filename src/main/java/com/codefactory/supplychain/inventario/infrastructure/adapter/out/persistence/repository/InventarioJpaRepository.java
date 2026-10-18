package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.InventarioJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    java.util.Optional<InventarioJpaEntity> findByNodoIdAndVarianteId(UUID nodoId, UUID varianteId);

    List<InventarioJpaEntity> findByNodoIdAndVarianteIdIn(UUID nodoId, Collection<UUID> varianteIds);

    Page<InventarioJpaEntity> findByNodoIdOrderByVarianteIdAsc(UUID nodoId, Pageable pageable);

    List<InventarioJpaEntity> findByVarianteIdOrderByNodoIdAsc(UUID varianteId);

    @Query("""
            SELECT i FROM InventarioJpaEntity i, NodoEntity n
             WHERE n.id = i.nodoId
               AND i.varianteId = :varianteId
               AND n.tiendaId IN :tiendaIds
             ORDER BY n.id""")
    List<InventarioJpaEntity> buscarPorVarianteEnTiendas(
            @Param("varianteId") UUID varianteId,
            @Param("tiendaIds") Collection<UUID> tiendaIds);
}
