package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Solo lectura por ahora (ver UsuarioRolRepositoryPort) — la asignación real de
 * roles a usuarios es HU-10.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Entity
@Table(name = "usuario_rol")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioRolEntity {

    @EmbeddedId
    private UsuarioRolId id;
}
