package com.codefactory.supplychain.identity.application.port.out;

/**
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
public interface TotpPort {

    /**
     * Genera un secreto TOTP nuevo (Base32), en texto plano. Quien lo invoque es
     * responsable de cifrarlo antes de persistirlo (ver SecretEncryptorPort).
     */
    String generarSecreto();

    /**
     * Construye la imagen QR (PNG, como data URI base64) que representa el secreto
     * para escanear con una app tipo Google Authenticator/Authy.
     */
    String generarQrDataUri(String secretoPlano, String emailUsuario);

    /**
     * Valida un código de 6 dígitos contra el secreto, con tolerancia a pequeños
     * desfaces de reloj entre el servidor y el dispositivo del usuario.
     */
    boolean verificarCodigo(String secretoPlano, String codigo);
}
