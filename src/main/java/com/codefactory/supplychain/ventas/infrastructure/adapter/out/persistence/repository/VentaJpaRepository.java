package com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.ventas.infrastructure.adapter.out.persistence.entity.VentaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentaJpaRepository extends JpaRepository<VentaEntity, UUID>,
        JpaSpecificationExecutor<VentaEntity> {

    List<VentaEntity> findByTransaccionIdOrderByVarianteId(UUID transaccionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VentaEntity v WHERE v.transaccionId = :transaccionId ORDER BY v.varianteId")
    List<VentaEntity> findByTransaccionIdParaActualizar(@Param("transaccionId") UUID transaccionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VentaEntity v WHERE v.id = :id")
    Optional<VentaEntity> buscarPorIdParaActualizar(@Param("id") UUID id);

    @Query(value = """
            SELECT 1
            FROM (SELECT pg_advisory_xact_lock(hashtextextended(CAST(:transaccionId AS text), 0))) lock_result
            """,
            nativeQuery = true)
    Integer bloquearTransaccion(@Param("transaccionId") UUID transaccionId);
}
