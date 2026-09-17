package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Password;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface PasswordHasherPort {

    PasswordHash hashear(Password password);

    /**
     * Compara una contraseña candidata en texto plano (tal como llega en un login,
     * SIN pasar por las invariantes de política de {@link Password}) contra un hash
     * ya almacenado. Deliberadamente no recibe un {@link Password} VO: la política de
     * longitud mínima se aplica al crear una contraseña nueva, no al validar una ya
     * existente (una cuenta creada antes de un cambio de política debe poder seguir
     * autenticándose).
     */
    boolean coincide(String passwordCandidata, PasswordHash hashAlmacenado);
}
