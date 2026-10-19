package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.VentanaQuiebreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentanaQuiebreJpaRepository
        extends JpaRepository<VentanaQuiebreEntity, UUID>, JpaSpecificationExecutor<VentanaQuiebreEntity> {

    Optional<VentanaQuiebreEntity> findByTiendaIdAndVarianteIdAndFechaFinIsNull(UUID tiendaId, UUID varianteId);

    List<VentanaQuiebreEntity> findByTiendaIdAndVarianteIdOrderByFechaInicioAsc(UUID tiendaId, UUID varianteId);

    List<VentanaQuiebreEntity> findByTiendaIdAndFechaFinIsNull(UUID tiendaId);

    @Query("select distinct v.varianteId from VentanaQuiebreEntity v where v.tiendaId = :tiendaId")
    List<UUID> findVarianteIdsByTiendaId(@Param("tiendaId") UUID tiendaId);

    /** Respeta el índice parcial único de V20: devuelve 0 si ya hay una ventana abierta para el par. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO ventana_quiebre (id, variante_id, tienda_id, fecha_inicio)
            VALUES (:id, :varianteId, :tiendaId, CAST(:fechaInicio AS timestamptz))
            ON CONFLICT (variante_id, tienda_id) WHERE fecha_fin IS NULL DO NOTHING
            """, nativeQuery = true)
    int abrirSiAusente(@Param("id") UUID id, @Param("varianteId") UUID varianteId,
                       @Param("tiendaId") UUID tiendaId, @Param("fechaInicio") Instant fechaInicio);
}
