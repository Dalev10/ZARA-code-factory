package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * codigo acepta tanto un TOTP de 6 dígitos como un código de respaldo alfanumérico
 * — a diferencia de ConfirmarMfaRequest (HU-07), acá no se restringe el formato
 * porque cualquiera de los dos es válido para completar el login.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record CompletarLoginMfaRequest(

        @Schema(description = "Token de desafío devuelto por /login", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "el token de desafío es obligatorio")
        String mfaChallengeToken,

        @Schema(description = "Código TOTP de 6 dígitos o código de respaldo de 10 caracteres", example = "492817")
        @NotBlank(message = "el código es obligatorio")
        String codigo) {
}
