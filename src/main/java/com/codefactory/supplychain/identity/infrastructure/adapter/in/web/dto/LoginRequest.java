package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A diferencia de RegistrarUsuarioRequest, la contraseña NO exige un mínimo de
 * caracteres aquí: eso es una regla de creación/cambio de contraseña, no de login
 * — una cuenta con una contraseña más corta que la política actual debe poder
 * seguir autenticándose.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record LoginRequest(

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        @Size(max = 255, message = "el email no puede superar 255 caracteres")
        String email,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(max = 128, message = "la contraseña no puede superar 128 caracteres")
        String password) {
}
