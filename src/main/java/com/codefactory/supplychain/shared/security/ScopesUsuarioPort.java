package com.codefactory.supplychain.shared.security;

import java.util.List;
import java.util.UUID;

/**
 * Puerto transversal implementado por el módulo identity (única fuente de
 * verdad sobre roles y scopes) y consumido acá para no invertir la
 * dependencia entre 'shared' y un módulo específico — mismo criterio que
 * VerificadorRolAdminPort, al que este reemplaza.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
public interface ScopesUsuarioPort {

    List<String> obtenerScopes(UUID usuarioId);
}
