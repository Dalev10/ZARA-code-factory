package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.SecretEncryptorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifra el secreto TOTP con AES-256/GCM antes de persistirlo. Cada llamada usa un IV
 * (nonce) aleatorio nuevo de 12 bytes, prependido al texto cifrado antes de codificar
 * en base64 — es lo que exige GCM para no reusar nunca el mismo (clave, IV).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class AesSecretEncryptorAdapter implements SecretEncryptorPort {

    private static final String TRANSFORMACION = "AES/GCM/NoPadding";
    private static final int LONGITUD_IV_BYTES = 12;
    private static final int LONGITUD_TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec clave;

    public AesSecretEncryptorAdapter(@Value("${app.security.mfa.encryption-key}") String claveBase64) {
        byte[] bytesClave = Base64.getDecoder().decode(claveBase64);
        this.clave = new SecretKeySpec(bytesClave, "AES");
    }

    @Override
    public String encriptar(String valorPlano) {
        try {
            byte[] iv = new byte[LONGITUD_IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LONGITUD_TAG_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(valorPlano.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + textoCifrado.length);
            buffer.put(iv).put(textoCifrado);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error cifrando el secreto de MFA", e);
        }
    }

    @Override
    public String desencriptar(String valorCifrado) {
        try {
            byte[] datos = Base64.getDecoder().decode(valorCifrado);
            byte[] iv = Arrays.copyOfRange(datos, 0, LONGITUD_IV_BYTES);
            byte[] textoCifrado = Arrays.copyOfRange(datos, LONGITUD_IV_BYTES, datos.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LONGITUD_TAG_BITS, iv));
            return new String(cipher.doFinal(textoCifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error descifrando el secreto de MFA", e);
        }
    }
}
