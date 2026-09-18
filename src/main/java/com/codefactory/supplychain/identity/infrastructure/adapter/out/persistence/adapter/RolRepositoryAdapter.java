package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.RolRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.Rol;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RolEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RolMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.RolJpaRepository;
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
public class RolRepositoryAdapter implements RolRepositoryPort {

    private final RolJpaRepository jpaRepository;
    private final RolMapper mapper;

    @Override
    public Rol guardar(Rol rol) {
        RolEntity guardada = jpaRepository.save(mapper.toEntity(rol));
        return mapper.toDomain(guardada);
    }

    @Override
    public Optional<Rol> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Rol> buscarPorNombre(String nombre) {
        return jpaRepository.findByNombre(nombre).map(mapper::toDomain);
    }

    @Override
    public List<Rol> listarTodos() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return jpaRepository.existsByNombre(nombre);
    }

    @Override
    public void eliminar(UUID id) {
        jpaRepository.deleteById(id);
    }
}
