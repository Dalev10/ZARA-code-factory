package com.codefactory.supplychain.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Chequeo provisorio de "solo admin" para endpoints sensibles (HU-09) mientras no
 * existe el guard genérico de autorización por scope (HU-11). Se expone como bean
 * con nombre fijo para poder referenciarlo desde SpEL en @PreAuthorize
 * ("@autorizacionAdmin.esAdmin(authentication)"), sin acoplar el filtro JWT
 * (JwtAuthenticationFilter) a esta consulta — ese filtro sigue sin poblar
 * autoridades a propósito, tal como quedó documentado ahí.
 *
 * Componentes transversales de tipo 'security', compartidos por todos los módulos.
 */
@Component("autorizacionAdmin")
@RequiredArgsConstructor
public class AutorizacionAdmin {

    private final VerificadorRolAdminPort verificadorRolAdminPort;

    public boolean esAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID usuarioId)) {
            return false;
        }
        return verificadorRolAdminPort.esAdmin(usuarioId);
    }
}
