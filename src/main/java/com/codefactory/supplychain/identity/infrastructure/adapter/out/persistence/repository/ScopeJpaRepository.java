package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.ScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface ScopeJpaRepository extends JpaRepository<ScopeEntity, UUID> {

    Optional<ScopeEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
