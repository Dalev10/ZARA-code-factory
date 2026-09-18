package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.domain.model.Usuario;

import java.time.Instant;

/**
 * El login puede terminar de dos formas: si la cuenta tiene MFA habilitado, el
 * primer paso solo entrega un token de desafío (RequiereMfa) — hace falta un
 * segundo paso (CompletarLoginMfaUseCase) para llegar a Completado. Si no tiene
 * MFA, login() devuelve Completado directamente.
 *
 * refreshTokenValor (en Completado) viaja en texto plano solo hasta el
 * controlador (para setear la cookie) — en base de datos únicamente se persiste
 * su hash.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public sealed interface LoginResultado {

    record RequiereMfa(String mfaChallengeToken) implements LoginResultado {
    }

    record Completado(String accessToken, String refreshTokenValor, Instant refreshTokenExpiraEn, Usuario usuario)
            implements LoginResultado {
    }
}
