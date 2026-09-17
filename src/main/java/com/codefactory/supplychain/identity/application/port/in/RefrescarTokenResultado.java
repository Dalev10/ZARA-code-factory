package com.codefactory.supplychain.identity.application.port.in;

import java.time.Instant;

/**
 * nuevoRefreshTokenValor viaja en texto plano solo hasta el controlador (para
 * reemplazar la cookie) — en base de datos únicamente se persiste su hash.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record RefrescarTokenResultado(String accessToken, String nuevoRefreshTokenValor,
                                       Instant nuevoRefreshTokenExpiraEn) {
}
