package com.codefactory.supplychain.identity.application.port.in;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record CrearScopeComando(String codigo, String descripcion, boolean sensible) {
}
