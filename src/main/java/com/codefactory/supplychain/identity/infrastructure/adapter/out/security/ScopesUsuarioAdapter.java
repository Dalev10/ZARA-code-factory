package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.shared.security.ScopesUsuarioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Resuelve usuario -> roles (usuario_rol) -> scopes (rol_scope) componiendo los
 * puertos que ya existen desde HU-09/HU-10, sin necesidad de una consulta SQL
 * nueva. Se llama en cada request (ver JwtAuthenticationFilter) para que un
 * scope quitado de un rol deje de aplicar de inmediato, sin esperar a que
 * expire el access token.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class ScopesUsuarioAdapter implements ScopesUsuarioPort {

    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort;
    private final RolScopeRepositoryPort rolScopeRepositoryPort;

    @Override
    public List<String> obtenerScopes(UUID usuarioId) {
        List<Rol> roles = usuarioRolRepositoryPort.listarRolesDeUsuario(usuarioId);
        return roles.stream()
                .flatMap(rol -> rolScopeRepositoryPort.listarScopesDeRol(rol.getId()).stream())
                .map(Scope::getCodigo)
                .distinct()
                .toList();
    }
}
