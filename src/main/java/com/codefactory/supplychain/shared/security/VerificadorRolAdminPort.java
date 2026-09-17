package com.codefactory.supplychain.shared.security;

import java.util.UUID;

/**
 * Puerto transversal implementado por el módulo identity (única fuente de verdad
 * sobre roles de usuario) y consumido acá para no invertir la dependencia entre
 * 'shared' y un módulo específico.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
public interface VerificadorRolAdminPort {

    boolean esAdmin(UUID usuarioId);
}
