package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioTiendaEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioTiendaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface UsuarioTiendaJpaRepository extends JpaRepository<UsuarioTiendaEntity, UsuarioTiendaId> {

    List<UsuarioTiendaEntity> findByIdUsuarioId(UUID usuarioId);
}
