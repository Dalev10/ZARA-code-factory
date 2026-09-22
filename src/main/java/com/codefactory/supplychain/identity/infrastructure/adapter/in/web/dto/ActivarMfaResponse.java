package com.codefactory.supplychain.identity.infrastructure.adapter.in.web.dto;

/**
 * qrCodeDataUri es una imagen PNG codificada en base64 ("data:image/png;base64,...")
 * — se puede pegar directo en la barra de un navegador o en un &lt;img&gt; para verla.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ActivarMfaResponse(String secreto, String qrCodeDataUri) {
}
