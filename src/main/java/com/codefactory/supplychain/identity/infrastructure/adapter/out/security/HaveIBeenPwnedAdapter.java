package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.application.port.out.PasswordComprometidaPort;
import com.codefactory.supplychain.identity.domain.model.Password;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Consulta la API de HaveIBeenPwned usando k-anonymity: solo se envían los primeros
 * 5 caracteres del hash SHA-1 de la contraseña, nunca la contraseña ni su hash completo.
 * Ver https://haveibeenpwned.com/API/v3#PwnedPasswords.
 *
 * Política fail-open (decisión confirmada): si el servicio no responde a tiempo o
 * hay un error de red, se asume que la contraseña NO está comprometida y se deja
 * continuar el registro — un tercero caído no debe bloquear la gestión de usuarios.
 *
 * Módulo: identity — Gestión de usuarios y autenticación (transversal, no forma parte del ERD de negocio)
 */
@Component
public class HaveIBeenPwnedAdapter implements PasswordComprometidaPort {

    private static final Logger log = LoggerFactory.getLogger(HaveIBeenPwnedAdapter.class);
    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;

    private final RestClient restClient;
    private final String rangeUrl;

    public HaveIBeenPwnedAdapter(
            @Value("${hibp.range-url:https://api.pwnedpasswords.com/range/{prefijo}}") String rangeUrl) {
        this.rangeUrl = rangeUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean estaComprometida(Password password) {
        try {
            String sha1 = sha1Hex(password.getValor());
            String prefijo = sha1.substring(0, 5);
            String sufijo = sha1.substring(5);

            String respuesta = restClient.get()
                    .uri(rangeUrl, prefijo)
                    .retrieve()
                    .body(String.class);

            return respuesta != null && respuesta.lines()
                    .anyMatch(linea -> linea.startsWith(sufijo + ":"));
        } catch (RestClientException excepcionDeRed) {
            log.warn("No se pudo verificar la contraseña contra HaveIBeenPwned; "
                    + "se continúa sin bloquear el registro (fail-open)", excepcionDeRed);
            return false;
        }
    }

    private static String sha1Hex(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no disponible en este JDK", e);
        }
    }
}
