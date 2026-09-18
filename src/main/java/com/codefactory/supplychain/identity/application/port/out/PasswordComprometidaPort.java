package com.codefactory.supplychain.identity.application.port.out;

import com.codefactory.supplychain.identity.domain.model.Password;

/**
 * Puerto hacia un servicio externo (HaveIBeenPwned) que indica si una contraseña
 * aparece en filtraciones conocidas. La política de qué hacer si el servicio no
 * responde (fail-open: no bloquear el registro) es responsabilidad del adaptador
 * que implemente este puerto, no de quien lo consume.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface PasswordComprometidaPort {

    boolean estaComprometida(Password password);
}
