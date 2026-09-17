package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.NoAutorizadoException;

/**
 * Se lanza tanto si el refresh token no existe, expiró, o ya fue usado antes
 * (reuse) — siempre con el mismo mensaje genérico, para no darle a un posible
 * atacante ninguna pista de cuál de esos casos ocurrió.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class TokenInvalidoException extends NoAutorizadoException {

    public TokenInvalidoException() {
        super("Sesión inválida o expirada");
    }
}
