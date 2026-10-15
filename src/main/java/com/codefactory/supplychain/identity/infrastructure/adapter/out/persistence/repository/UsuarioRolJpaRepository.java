package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface UsuarioRolJpaRepository extends JpaRepository<UsuarioRolEntity, UsuarioRolId> {

    boolean existsByIdRolId(UUID rolId);

    List<UsuarioRolEntity> findByIdUsuarioId(UUID usuarioId);

    @Query(value = """
            SELECT count(*) FROM usuario_rol ur
            JOIN usuario u ON u.id = ur.usuario_id
            WHERE ur.rol_id = :rolId AND u.estado = 'ACTIVO'
            """, nativeQuery = true)
    long contarUsuariosActivosConRol(@Param("rolId") UUID rolId);
}
