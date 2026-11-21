package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.CicloDespachoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Repositorio Spring Data de {@link CicloDespachoEntity}. */
public interface CicloDespachoJpaRepository extends JpaRepository<CicloDespachoEntity, UUID> {

    String FILTRO = """
            FROM ciclo_despacho c
            WHERE (CAST(:ordenId AS uuid) IS NULL OR c.orden_id = CAST(:ordenId AS uuid))
              AND (:sinViaje = false
                   OR NOT EXISTS (SELECT 1 FROM viaje_parada vp WHERE vp.ciclo_despacho_id = c.id))
            """;

    @Query("SELECT COALESCE(MAX(c.numeroCiclo), 0) FROM CicloDespachoEntity c WHERE c.ordenId = :ordenId")
    int ultimoNumeroCiclo(@Param("ordenId") UUID ordenId);

    /** SQL nativo: el filtro por viaje mira {@code viaje_parada}, que no tiene entidad en este módulo todavía. */
    @Query(value = "SELECT c.* " + FILTRO + " ORDER BY c.fecha_despacho DESC, c.id",
            countQuery = "SELECT count(*) " + FILTRO, nativeQuery = true)
    Page<CicloDespachoEntity> buscar(@Param("ordenId") UUID ordenId, @Param("sinViaje") boolean sinViaje,
                                     Pageable pageable);
}
