package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.UsuarioRolRepositoryPort;
import com.codefactory.supplychain.shared.security.VerificadorRolAdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
@RequiredArgsConstructor
public class VerificadorRolAdminAdapter implements VerificadorRolAdminPort {

    private static final String NOMBRE_ROL_ADMIN = "ADMIN";

    private final UsuarioRolRepositoryPort usuarioRolRepositoryPort;

    @Override
    public boolean esAdmin(UUID usuarioId) {
        return usuarioRolRepositoryPort.usuarioTieneRolNombrado(usuarioId, NOMBRE_ROL_ADMIN);
    }
}
