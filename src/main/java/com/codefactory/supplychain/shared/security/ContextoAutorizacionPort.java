package com.codefactory.supplychain.shared.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto transversal implementado por el módulo identity (única fuente de verdad sobre
 * usuarios, roles y scopes) y consumido por JwtAuthenticationFilter, para no invertir la
 * dependencia entre 'shared' y un módulo específico.
 *
 * Se resuelve en cada request: quitarle un scope a un rol o desactivar a un usuario
 * aplica de inmediato, sin esperar a que expire el access token.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
public interface ContextoAutorizacionPort {

    /**
     * Vacío si el usuario no existe o no está ACTIVO: el request sigue sin autenticar.
     */
    Optional<UsuarioActual> resolver(UUID usuarioId);
}
