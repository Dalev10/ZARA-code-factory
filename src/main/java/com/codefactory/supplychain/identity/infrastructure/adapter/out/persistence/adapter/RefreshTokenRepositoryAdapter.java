package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.adapter;

import com.codefactory.supplychain.identity.application.port.out.RefreshTokenRepositoryPort;
import com.codefactory.supplychain.identity.domain.model.RefreshToken;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    @Override
    public RefreshToken guardar(RefreshToken refreshToken) {
        RefreshTokenEntity guardado = jpaRepository.save(mapper.toEntity(refreshToken));
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<RefreshToken> buscarPorTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revocarFamilia(UUID familiaId, Instant ahora) {
        jpaRepository.revocarFamilia(familiaId, ahora);
    }
}
