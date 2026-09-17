package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.RefreshToken;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface RefreshTokenRepositoryPort {

    RefreshToken guardar(RefreshToken refreshToken);
}
