package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record RegistrarUsuarioRequest(

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato válido")
        @Size(max = 255, message = "el email no puede superar 255 caracteres")
        String email,

        @NotBlank(message = "el nombre completo es obligatorio")
        @Size(max = 150, message = "el nombre completo no puede superar 150 caracteres")
        String nombreCompleto,

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 12, max = 128, message = "la contraseña debe tener entre 12 y 128 caracteres")
        String password) {
}
