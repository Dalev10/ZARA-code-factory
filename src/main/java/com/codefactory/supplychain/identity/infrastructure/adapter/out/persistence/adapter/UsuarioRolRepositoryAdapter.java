package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.RolJpaRepository;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioRolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class UsuarioRolRepositoryAdapter implements UsuarioRolRepositoryPort {

    private final UsuarioRolJpaRepository usuarioRolJpaRepository;
    private final RolJpaRepository rolJpaRepository;

    @Override
    public boolean tieneUsuariosAsignados(UUID rolId) {
        return usuarioRolJpaRepository.existsByIdRolId(rolId);
    }

    @Override
    public boolean usuarioTieneRolNombrado(UUID usuarioId, String nombreRol) {
        return rolJpaRepository.findByNombre(nombreRol)
                .map(rol -> usuarioRolJpaRepository.existsById(new UsuarioRolId(usuarioId, rol.getId())))
                .orElse(false);
    }
}
