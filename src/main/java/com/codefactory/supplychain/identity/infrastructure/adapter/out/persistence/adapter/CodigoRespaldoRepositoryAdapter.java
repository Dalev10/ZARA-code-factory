package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.CodigoRespaldoRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.CodigoRespaldo;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.CodigoRespaldoEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.CodigoRespaldoMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.CodigoRespaldoJpaRepository;
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
public class CodigoRespaldoRepositoryAdapter implements CodigoRespaldoRepositoryPort {

    private final CodigoRespaldoJpaRepository jpaRepository;
    private final CodigoRespaldoMapper mapper;

    @Override
    public List<CodigoRespaldo> guardarTodos(List<CodigoRespaldo> codigos) {
        List<CodigoRespaldoEntity> entidades = codigos.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entidades).stream().map(mapper::toDomain).toList();
    }

    @Override
    public CodigoRespaldo guardar(CodigoRespaldo codigo) {
        CodigoRespaldoEntity guardado = jpaRepository.save(mapper.toEntity(codigo));
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<CodigoRespaldo> buscarNoUsadoPorHash(UUID usuarioId, String codigoHash) {
        return jpaRepository.findByUsuarioIdAndCodigoHashAndUsadoEnIsNull(usuarioId, codigoHash)
                .map(mapper::toDomain);
    }
}
