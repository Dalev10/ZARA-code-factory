package com.codefactory.supplychain.identity.application.port.out;

import java.util.UUID;

/**
 * Una tienda tal como la ve identity al asignarla a usuarios (ver TiendaConsultaPort).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record TiendaAsignable(UUID id, String nombre, boolean activa) {
}
