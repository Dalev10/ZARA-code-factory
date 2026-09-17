package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.RolScopeRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RolScopeEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RolScopeId;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.ScopeMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.RolScopeJpaRepository;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.ScopeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class RolScopeRepositoryAdapter implements RolScopeRepositoryPort {

    private final RolScopeJpaRepository rolScopeJpaRepository;
    private final ScopeJpaRepository scopeJpaRepository;
    private final ScopeMapper scopeMapper;

    @Override
    public void asignar(UUID rolId, UUID scopeId) {
        rolScopeJpaRepository.save(new RolScopeEntity(new RolScopeId(rolId, scopeId)));
    }

    @Override
    public void quitar(UUID rolId, UUID scopeId) {
        rolScopeJpaRepository.deleteById(new RolScopeId(rolId, scopeId));
    }

    @Override
    public boolean existeAsignacion(UUID rolId, UUID scopeId) {
        return rolScopeJpaRepository.existsById(new RolScopeId(rolId, scopeId));
    }

    @Override
    public List<Scope> listarScopesDeRol(UUID rolId) {
        List<UUID> scopeIds = rolScopeJpaRepository.findByIdRolId(rolId).stream()
                .map(entity -> entity.getId().getScopeId())
                .toList();
        return scopeJpaRepository.findAllById(scopeIds).stream().map(scopeMapper::toDomain).toList();
    }

    @Override
    public boolean tieneRolesAsignados(UUID scopeId) {
        return rolScopeJpaRepository.existsByIdScopeId(scopeId);
    }
}
