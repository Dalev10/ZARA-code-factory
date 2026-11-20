package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.reposicion.domain.model.EstadoRecomendacion;
import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.RecomendacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

/**
 * Repositorio JPA para {@code recomendacion}.
 *
 * Módulo: reposicion.
 */
public interface RecomendacionJpaRepository extends JpaRepository<RecomendacionEntity, UUID> {

    boolean existsByTiendaIdAndVarianteIdAndEstado(UUID tiendaId, UUID varianteId, EstadoRecomendacion estado);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM orden_reabastecimiento
                WHERE tienda_id = :tiendaId AND variante_id = :varianteId
                  AND estado = 'GENERADA' AND cd_asignado_id IS NULL)
            """, nativeQuery = true)
    boolean existeOrdenGeneradaSinCd(@Param("tiendaId") UUID tiendaId, @Param("varianteId") UUID varianteId);

    @Query("""
            SELECT r FROM RecomendacionEntity r
            WHERE (:estado IS NULL OR r.estado = :estado)
              AND (:tiendaId IS NULL OR r.tiendaId = :tiendaId)
              AND (:varianteId IS NULL OR r.varianteId = :varianteId)
              AND (:todas = true OR r.tiendaId IN :tiendas)
            ORDER BY r.fechaCreacion DESC
            """)
    Page<RecomendacionEntity> listar(@Param("estado") EstadoRecomendacion estado,
                                     @Param("tiendaId") UUID tiendaId,
                                     @Param("varianteId") UUID varianteId,
                                     @Param("todas") boolean todas,
                                     @Param("tiendas") Collection<UUID> tiendas,
                                     Pageable pageable);
}
