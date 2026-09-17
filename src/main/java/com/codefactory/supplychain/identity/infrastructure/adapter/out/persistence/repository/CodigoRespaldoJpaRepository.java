package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.CodigoRespaldoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface CodigoRespaldoJpaRepository extends JpaRepository<CodigoRespaldoEntity, UUID> {

    Optional<CodigoRespaldoEntity> findByUsuarioIdAndCodigoHashAndUsadoEnIsNull(UUID usuarioId, String codigoHash);
}
