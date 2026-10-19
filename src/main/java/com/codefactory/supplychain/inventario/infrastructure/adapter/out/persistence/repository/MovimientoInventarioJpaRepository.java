package com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.inventario.infrastructure.adapter.out.persistence.entity.MovimientoInventarioEntity;
import com.codefactory.supplychain.inventario.domain.model.TipoMovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA del kardex de Inventario.
 */
public interface MovimientoInventarioJpaRepository
        extends JpaRepository<MovimientoInventarioEntity, UUID> {

    @Query(value = """
            SELECT m FROM MovimientoInventarioEntity m
             WHERE m.nodoId = :nodoId
               AND (:tipo IS NULL OR m.tipo = :tipo)
               AND m.ocurridoEn >= :desde AND m.ocurridoEn <= :hasta
             ORDER BY m.ocurridoEn, m.id""",
            countQuery = """
            SELECT COUNT(m) FROM MovimientoInventarioEntity m
             WHERE m.nodoId = :nodoId
               AND (:tipo IS NULL OR m.tipo = :tipo)
               AND m.ocurridoEn >= :desde AND m.ocurridoEn <= :hasta""")
    Page<MovimientoInventarioEntity> buscarKardex(
            @Param("nodoId") UUID nodoId,
            @Param("tipo") TipoMovimientoInventario tipo,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            Pageable pageable);

    List<MovimientoInventarioEntity> findByNodoIdAndVarianteIdAndOcurridoEnBetweenOrderByOcurridoEnAscIdAsc(
            UUID nodoId, UUID varianteId, Instant desde, Instant hasta);
}
