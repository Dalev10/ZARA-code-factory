package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ConfirmarMfaRequest(

        @NotBlank(message = "el código es obligatorio")
        @Pattern(regexp = "\\d{6}", message = "el código debe tener exactamente 6 dígitos")
        String codigo) {
}
