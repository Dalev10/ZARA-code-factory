package com.codefactory.supplychain.identity.application.port.in;

import com.codefactory.supplychain.identity.domain.model.Usuario;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface DesactivarUsuarioUseCase {

    Usuario desactivar(DesactivarUsuarioComando comando);
}
