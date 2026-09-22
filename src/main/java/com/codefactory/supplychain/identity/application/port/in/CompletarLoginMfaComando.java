package com.codefactory.supplychain.identity.application.port.in;

/**
 * codigo acepta tanto un código TOTP de 6 dígitos como un código de respaldo
 * alfanumérico de HU-07 — el servicio prueba ambos.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record CompletarLoginMfaComando(String mfaChallengeToken, String codigo) {
}
