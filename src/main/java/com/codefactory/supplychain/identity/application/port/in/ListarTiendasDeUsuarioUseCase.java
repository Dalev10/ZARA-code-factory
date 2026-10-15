package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.application.port.out.TiendaAsignable;

import java.util.List;
import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface ListarTiendasDeUsuarioUseCase {

    List<TiendaAsignable> listarTiendas(UUID usuarioId);
}
