package com.codefactory.supplychain.identity.application.port.out;

import java.util.UUID;

/**
 * Token de vida muy corta que prueba "esta persona ya presentó la contraseña
 * correcta para esta cuenta", emitido tras el primer paso del login cuando la
 * cuenta tiene MFA habilitado. Se intercambia por los tokens reales en el segundo
 * paso (POST /api/v1/auth/login/mfa), junto con el código TOTP o un código de
 * respaldo. Nunca sirve como access token (ver shared.security.JwtClaimTypes).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface MfaChallengeTokenPort {

    String generar(UUID usuarioId);

    /**
     * @throws com.codefactory.supplychain.identity.domain.exception.TokenInvalidoException
     *         si el token es inválido, expiró, o no es realmente un token de desafío MFA.
     */
    UUID validar(String challengeToken);
}
