package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.RecepcionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data de {@link RecepcionEntity}.
 *
 * Módulo: logistica — Recepción en la Bodega_Tienda (HU-49).
 */
public interface RecepcionJpaRepository extends JpaRepository<RecepcionEntity, UUID> {

    Optional<RecepcionEntity> findByViajeParadaId(UUID viajeParadaId);
}
