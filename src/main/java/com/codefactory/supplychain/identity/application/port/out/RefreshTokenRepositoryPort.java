package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface RefreshTokenRepositoryPort {

    RefreshToken guardar(RefreshToken refreshToken);

    Optional<RefreshToken> buscarPorTokenHash(String tokenHash);

    /**
     * Revoca de una sola vez todos los tokens activos (no revocados) de una familia.
     * Se usa cuando se detecta el reuse de un token ya rotado: se asume un posible robo
     * y se invalida toda la cadena, forzando un nuevo login.
     */
    void revocarFamilia(UUID familiaId, Instant ahora);
}
