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
 * {@code puedeOperarEn} restringe por tienda (HU-32): las operaciones sobre una tienda
 * concreta exigen el scope de la operación y, además, poder operar en esa tienda.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
public record UsuarioActual(UUID id, Set<String> scopes, Set<UUID> tiendasPermitidas) {

    /**
     * Acceso global: opera en todas las tiendas sin necesidad de asignaciones. Se deduce
     * del scope en lugar de guardarse aparte, para que no haya dos datos que se contradigan.
     */
    public static final String SCOPE_OPERAR_TODAS_LAS_TIENDAS = "tiendas:operar-todas";

    public UsuarioActual {
        scopes = Set.copyOf(scopes);
        tiendasPermitidas = Set.copyOf(tiendasPermitidas);
    }

    public boolean tieneScope(String codigo) {
        return scopes.contains(codigo);
    }

    public boolean puedeOperarEn(UUID tiendaId) {
        return tieneScope(SCOPE_OPERAR_TODAS_LAS_TIENDAS) || tiendasPermitidas.contains(tiendaId);
    }
}
