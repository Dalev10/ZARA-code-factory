package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

/**
 * Cuando mfaRequerido es true, solo viene mfaChallengeToken (hay que llamar a
 * POST /api/v1/auth/login/mfa con ese token + el código); accessToken/usuario
 * quedan en null. Cuando es false, es al revés. El refresh token NUNCA aparece
 * acá — viaja solo en la cookie HttpOnly.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record LoginResponse(boolean mfaRequerido, String mfaChallengeToken, String accessToken,
                             UsuarioResponse usuario) {

    public static LoginResponse requiereMfa(String mfaChallengeToken) {
        return new LoginResponse(true, mfaChallengeToken, null, null);
    }

    public static LoginResponse completado(String accessToken, UsuarioResponse usuario) {
        return new LoginResponse(false, null, accessToken, usuario);
    }
}
