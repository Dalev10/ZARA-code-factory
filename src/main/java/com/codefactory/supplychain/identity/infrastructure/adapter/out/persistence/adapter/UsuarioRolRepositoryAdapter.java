package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioRolId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RolMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.RolJpaRepository;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioRolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class UsuarioRolRepositoryAdapter implements UsuarioRolRepositoryPort {

    private final UsuarioRolJpaRepository usuarioRolJpaRepository;
    private final RolJpaRepository rolJpaRepository;
    private final RolMapper rolMapper;

    @Override
    public boolean tieneUsuariosAsignados(UUID rolId) {
        return usuarioRolJpaRepository.existsByIdRolId(rolId);
    }

    @Override
    public void asignar(UUID usuarioId, UUID rolId) {
        usuarioRolJpaRepository.save(new UsuarioRolEntity(new UsuarioRolId(usuarioId, rolId)));
    }

    @Override
    public void quitar(UUID usuarioId, UUID rolId) {
        usuarioRolJpaRepository.deleteById(new UsuarioRolId(usuarioId, rolId));
    }

    @Override
    public boolean existeAsignacion(UUID usuarioId, UUID rolId) {
        return usuarioRolJpaRepository.existsById(new UsuarioRolId(usuarioId, rolId));
    }

    @Override
    public List<Rol> listarRolesDeUsuario(UUID usuarioId) {
        List<UUID> rolIds = usuarioRolJpaRepository.findByIdUsuarioId(usuarioId).stream()
                .map(entity -> entity.getId().getRolId())
                .toList();
        return rolJpaRepository.findAllById(rolIds).stream().map(rolMapper::toDomain).toList();
    }

    @Override
    public long contarUsuariosConRol(UUID rolId) {
        return usuarioRolJpaRepository.countByIdRolId(rolId);
    }
}
