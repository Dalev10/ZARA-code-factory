package com.codefactory.supplychain.identity.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HU-32: en qué tiendas puede operar un usuario. Solo guarda el id de la tienda: sus datos
 * los provee inventario por su API pública (identity nunca lee la tabla tienda).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Entity
@Table(name = "usuario_tienda")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioTiendaEntity {

    @EmbeddedId
    private UsuarioTiendaId id;
}
