package com.codefactory.supplychain.identity.application.port.out;

/**
 * Cifrado simétrico y reversible (a diferencia de PasswordHasherPort, que es de un
 * solo sentido) — el secreto TOTP se necesita en texto plano más adelante para
 * validar códigos, así que hashearlo no serviría.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface SecretEncryptorPort {

    String encriptar(String valorPlano);

    String desencriptar(String valorCifrado);
}
