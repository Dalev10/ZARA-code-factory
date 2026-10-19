package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.SolicitudNoSatisfechaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SolicitudNoSatisfechaJpaRepository
        extends JpaRepository<SolicitudNoSatisfechaEntity, UUID>, JpaSpecificationExecutor<SolicitudNoSatisfechaEntity> {

    Optional<SolicitudNoSatisfechaEntity> findByClaveIdempotencia(String claveIdempotencia);

    /** Atómico frente a peticiones concurrentes con la misma clave: devuelve 0 si ya existía. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO solicitud_no_satisfecha
                (id, variante_id, tienda_id, cantidad, stock_sistema, timestamp_solicitud,
                 registrado_por_id, clave_idempotencia, estado)
            VALUES (:id, :varianteId, :tiendaId, :cantidad, CAST(:stockSistema AS integer),
                    CAST(:registradaEn AS timestamptz), :registradoPor, :clave, 'ACTIVA')
            ON CONFLICT (clave_idempotencia) DO NOTHING
            """, nativeQuery = true)
    int insertarSiAusente(@Param("id") UUID id, @Param("varianteId") UUID varianteId,
                          @Param("tiendaId") UUID tiendaId, @Param("cantidad") int cantidad,
                          @Param("stockSistema") Integer stockSistema, @Param("registradaEn") Instant registradaEn,
                          @Param("registradoPor") UUID registradoPor, @Param("clave") String clave);
}
