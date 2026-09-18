package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import com.codefactory.supplychain.identity.domain.model.Usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Nunca incluye el hash de contraseña ni el secreto MFA — solo los datos que la API
 * debe exponer de un usuario.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record UsuarioResponse(UUID id, String email, String nombreCompleto, String estado, Instant creadoEn) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getEmail().getValor(),
                usuario.getNombreCompleto(),
                usuario.getEstado().name(),
                usuario.getCreadoEn());
    }
}
