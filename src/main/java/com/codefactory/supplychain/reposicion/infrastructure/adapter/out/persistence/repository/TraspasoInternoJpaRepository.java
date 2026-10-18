package com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.reposicion.infrastructure.adapter.out.persistence.entity.TraspasoInternoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para {@code traspaso_interno}.
 *
 * Módulo: reposicion — Reposición interna.
 */
public interface TraspasoInternoJpaRepository extends JpaRepository<TraspasoInternoEntity, UUID> {
    List<TraspasoInternoEntity> findByTiendaIdOrderByTimestampTraspasoDesc(UUID tiendaId);
}
