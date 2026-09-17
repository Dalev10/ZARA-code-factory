package com.codefactory.supplychain.identity.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generación y hasheo de valores opacos de alta entropía (refresh tokens, códigos
 * de respaldo de MFA) — extraído acá para no duplicar la lógica criptográfica entre
 * los distintos servicios de application que la necesitan.
 *
 * No es un puerto hexagonal a propósito: SecureRandom y SHA-256 son primitivas del
 * JDK, no una dependencia de infraestructura intercambiable (mismo criterio que
 * UUID.randomUUID() en los agregados de dominio).
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
final class HashingSupport {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALFABETO_CODIGO_RESPALDO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private HashingSupport() {
    }

    static String generarValorAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Código alfanumérico legible para tipear a mano (códigos de respaldo de MFA).
     * Excluye caracteres ambiguos (0/O, 1/I/L) del alfabeto.
     */
    static String generarCodigoAlfanumerico(int longitud) {
        StringBuilder codigo = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            codigo.append(ALFABETO_CODIGO_RESPALDO.charAt(RANDOM.nextInt(ALFABETO_CODIGO_RESPALDO.length())));
        }
        return codigo.toString();
    }

    static String sha256Hex(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este JDK", e);
        }
    }
}
