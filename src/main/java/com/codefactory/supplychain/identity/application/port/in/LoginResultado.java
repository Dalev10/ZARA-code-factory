package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.domain.model.Usuario;

import java.time.Instant;

/**
 * refreshTokenValor viaja en texto plano solo hasta el controlador (para setear la
 * cookie) — en base de datos únicamente se persiste su hash.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record LoginResultado(String accessToken, String refreshTokenValor, Instant refreshTokenExpiraEn,
                              Usuario usuario) {
}
