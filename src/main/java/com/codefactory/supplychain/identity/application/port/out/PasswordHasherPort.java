package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface PasswordHasherPort {

    PasswordHash hashear(Password password);
}
