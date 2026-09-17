package com.codefactory.supplychain.identity.application.port.in;

/**
 * secretoBase32 se muestra para ingreso manual (alternativa a escanear el QR).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public record ActivarMfaResultado(String secretoBase32, String qrCodeDataUri) {
}
