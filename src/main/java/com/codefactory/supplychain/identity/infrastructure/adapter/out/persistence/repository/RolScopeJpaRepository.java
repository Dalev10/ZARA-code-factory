package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RolScopeEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RolScopeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface RolScopeJpaRepository extends JpaRepository<RolScopeEntity, RolScopeId> {

    List<RolScopeEntity> findByIdRolId(UUID rolId);

    boolean existsByIdScopeId(UUID scopeId);
}
