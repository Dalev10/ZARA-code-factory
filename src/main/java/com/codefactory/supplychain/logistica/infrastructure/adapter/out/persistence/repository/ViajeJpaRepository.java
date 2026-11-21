package com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.logistica.domain.model.EstadoViaje;
import com.codefactory.supplychain.logistica.infrastructure.adapter.out.persistence.entity.ViajeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Repositorio Spring Data de {@link ViajeEntity}. */
public interface ViajeJpaRepository extends JpaRepository<ViajeEntity, UUID> {

    Page<ViajeEntity> findByEstado(EstadoViaje estado, Pageable pageable);

    @Query("SELECT COUNT(p) > 0 FROM ViajeParadaEntity p WHERE p.cicloDespachoId = :cicloId")
    boolean existeParadaDelCiclo(@Param("cicloId") UUID cicloId);
}
