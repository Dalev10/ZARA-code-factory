package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ScopeRequest(

        @NotBlank(message = "el código del scope es obligatorio")
        @Size(max = 100, message = "el código del scope no puede superar 100 caracteres")
        String codigo,

        @Size(max = 255, message = "la descripción no puede superar 255 caracteres")
        String descripcion,

        boolean sensible) {
}
