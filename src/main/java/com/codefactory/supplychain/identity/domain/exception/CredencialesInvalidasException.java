package com.codefactory.supplychain.identity.domain.exception;

import com.codefactory.supplychain.shared.exception.NoAutorizadoException;

/**
 * Se lanza tanto si el email no existe, la contraseña no coincide, o la cuenta
 * no está ACTIVA — siempre con el mismo mensaje genérico, para no revelar a un
 * atacante si una cuenta existe o por qué fue rechazada (evita enumeración de usuarios).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public class CredencialesInvalidasException extends NoAutorizadoException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
