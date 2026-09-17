package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

/**
 * El refresh token NUNCA aparece acá — viaja solo en la cookie HttpOnly, nunca
 * en el cuerpo de la respuesta ni accesible por JavaScript.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record LoginResponse(String accessToken, UsuarioResponse usuario) {
}
