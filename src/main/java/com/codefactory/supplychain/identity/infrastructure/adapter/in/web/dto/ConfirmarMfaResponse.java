package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import java.util.List;

/**
 * codigosRespaldo se muestra una única vez — el backend solo guarda su hash.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ConfirmarMfaResponse(List<String> codigosRespaldo) {
}
