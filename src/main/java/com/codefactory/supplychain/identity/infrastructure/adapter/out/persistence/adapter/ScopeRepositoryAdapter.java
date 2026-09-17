package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.ScopeRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Scope;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.ScopeEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.ScopeMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.ScopeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class ScopeRepositoryAdapter implements ScopeRepositoryPort {

    private final ScopeJpaRepository jpaRepository;
    private final ScopeMapper mapper;

    @Override
    public Scope guardar(Scope scope) {
        ScopeEntity guardada = jpaRepository.save(mapper.toEntity(scope));
        return mapper.toDomain(guardada);
    }

    @Override
    public Optional<Scope> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Scope> buscarPorCodigo(String codigo) {
        return jpaRepository.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    public List<Scope> listarTodos() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return jpaRepository.existsByCodigo(codigo);
    }

    @Override
    public void eliminar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
