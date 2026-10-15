package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository;

import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Estado y scopes del usuario en una sola consulta (se ejecuta en cada request). Una
     * fila por scope; si el usuario no tiene roles ni scopes, una sola fila con codigo NULL;
     * si no existe, ninguna fila.
     */
    @Query(value = """
            SELECT u.estado AS estado, s.codigo AS codigo
            FROM usuario u
            LEFT JOIN usuario_rol ur ON ur.usuario_id = u.id
            LEFT JOIN rol_scope rs ON rs.rol_id = ur.rol_id
            LEFT JOIN scope s ON s.id = rs.scope_id
            WHERE u.id = :usuarioId
            """, nativeQuery = true)
    List<FilaContextoAutorizacion> buscarContextoAutorizacion(@Param("usuarioId") UUID usuarioId);

    interface FilaContextoAutorizacion {
        String getEstado();

        String getCodigo();
    }
}
