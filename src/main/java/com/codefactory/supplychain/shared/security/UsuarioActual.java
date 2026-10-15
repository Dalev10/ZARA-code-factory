package com.codefactory.supplychain.shared.security;

import java.util.Set;
import java.util.UUID;

/**
 * Usuario autenticado del request, armado por JwtAuthenticationFilter y disponible como
 * principal del SecurityContext. Los controladores lo reciben con
 * {@code @AuthenticationPrincipal} y lo pasan en el comando: el id nunca sale del body.
 *
 * {@code tieneScope} cubre los casos que un {@code @PreAuthorize} fijo no puede expresar,
 * porque el scope requerido depende de un dato que solo se conoce al cargar el recurso.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
public record UsuarioActual(UUID id, Set<String> scopes) {

    public UsuarioActual {
        scopes = Set.copyOf(scopes);
    }

    public boolean tieneScope(String codigo) {
        return scopes.contains(codigo);
    }
}
