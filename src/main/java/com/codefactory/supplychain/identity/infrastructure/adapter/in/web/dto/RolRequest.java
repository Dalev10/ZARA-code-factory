package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record RolRequest(

        @NotBlank(message = "el nombre del rol es obligatorio")
        @Size(max = 100, message = "el nombre del rol no puede superar 100 caracteres")
        String nombre,

        @Size(max = 255, message = "la descripción no puede superar 255 caracteres")
        String descripcion) {
}
