package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.mapper;

import com.codefactory.supplychain.identity.domain.model.Email;
import com.codefactory.supplychain.identity.domain.model.PasswordHash;
import com.codefactory.supplychain.identity.domain.model.Usuario;
import com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class UsuarioMapper {

    public UsuarioEntity toEntity(Usuario usuario) {
        return UsuarioEntity.builder()
                .id(usuario.getId())
                .email(usuario.getEmail().getValor())
                .nombreCompleto(usuario.getNombreCompleto())
                .passwordHash(usuario.getPasswordHash().getValor())
                .estado(usuario.getEstado())
                .intentosFallidos(usuario.getIntentosFallidos())
                .bloqueadoHasta(usuario.getBloqueadoHasta())
                .mfaHabilitado(usuario.isMfaHabilitado())
                .mfaSecretEncrypted(usuario.getMfaSecretEncrypted())
                .proveedorExterno(usuario.getProveedorExterno())
                .creadoEn(usuario.getCreadoEn())
                .actualizadoEn(usuario.getActualizadoEn())
                .build();
    }

    public Usuario toDomain(UsuarioEntity entity) {
        return Usuario.reconstruir(
                entity.getId(),
                Email.de(entity.getEmail()),
                entity.getNombreCompleto(),
                PasswordHash.de(entity.getPasswordHash()),
                entity.getEstado(),
                entity.getIntentosFallidos(),
                entity.getBloqueadoHasta(),
                entity.isMfaHabilitado(),
                entity.getMfaSecretEncrypted(),
                entity.getProveedorExterno(),
                entity.getCreadoEn(),
                entity.getActualizadoEn());
    }
}
