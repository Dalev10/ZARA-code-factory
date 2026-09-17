package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

/**
 * El nuevo refresh token NUNCA aparece acá — viaja solo en la cookie HttpOnly reemplazada.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record RefrescarTokenResponse(String accessToken) {
}
