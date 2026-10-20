package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.UsuarioTiendaRepositoryPort;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioTiendaEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioTiendaId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.UsuarioTiendaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class UsuarioTiendaRepositoryAdapter implements UsuarioTiendaRepositoryPort {

    private final UsuarioTiendaJpaRepository jpaRepository;

    @Override
    public void asignar(UUID usuarioId, UUID tiendaId) {
        jpaRepository.save(new UsuarioTiendaEntity(new UsuarioTiendaId(usuarioId, tiendaId)));
    }

    @Override
    public void quitar(UUID usuarioId, UUID tiendaId) {
        jpaRepository.deleteById(new UsuarioTiendaId(usuarioId, tiendaId));
    }

    @Override
    public boolean existeAsignacion(UUID usuarioId, UUID tiendaId) {
        return jpaRepository.existsById(new UsuarioTiendaId(usuarioId, tiendaId));
    }

    @Override
    public List<UUID> listarTiendasDeUsuario(UUID usuarioId) {
        return jpaRepository.findByIdUsuarioId(usuarioId).stream().map(e -> e.getId().getTiendaId()).toList();
    }
}
