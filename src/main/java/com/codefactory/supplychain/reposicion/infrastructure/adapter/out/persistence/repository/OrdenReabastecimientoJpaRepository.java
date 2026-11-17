package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.reposicion.domain.model.EstadoOrden;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.OrdenReabastecimientoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

/**
 * Repositorio JPA para {@code orden_reabastecimiento}.
 *
 * Módulo: reposicion.
 */
public interface OrdenReabastecimientoJpaRepository extends JpaRepository<OrdenReabastecimientoEntity, UUID> {

    @Query("""
            SELECT o FROM OrdenReabastecimientoEntity o
            WHERE (:estado IS NULL OR o.estado = :estado)
              AND (:tiendaId IS NULL OR o.tiendaId = :tiendaId)
              AND (:todas = true OR o.tiendaId IN :tiendas)
            ORDER BY o.fechaCreacion DESC
            """)
    Page<OrdenReabastecimientoEntity> listar(@Param("estado") EstadoOrden estado,
                                             @Param("tiendaId") UUID tiendaId,
                                             @Param("todas") boolean todas,
                                             @Param("tiendas") Collection<UUID> tiendas,
                                             Pageable pageable);
}
