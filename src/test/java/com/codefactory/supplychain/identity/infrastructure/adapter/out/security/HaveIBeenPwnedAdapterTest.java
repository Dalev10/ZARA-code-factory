package com.codefactory.supplychain.identity.infrastructure.adapter.out.security;

import com.codefactory.supplychain.identity.domain.model.Password;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Levanta un servidor HTTP local (sin dependencias nuevas, usando com.sun.net.httpserver
 * del propio JDK) que imita el endpoint de rango de HaveIBeenPwned, para probar la
 * lógica de k-anonymity y la política fail-open sin depender de la red real.
 */
class HaveIBeenPwnedAdapterTest {

    private HttpServer servidor;

    @AfterEach
    void tearDown() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    @Test
    void detectaUnaContraseñaComprometidaCuandoElSufijoApareceEnLaRespuesta() throws Exception {
        String passwordFiltrada = "password12345";
        String sha1 = sha1Hex(passwordFiltrada);
        String sufijo = sha1.substring(5);

        servidor = iniciarServidorConRespuesta(sufijo + ":12345\nOTROSUFIJO0000000000000000000000000:1");
        HaveIBeenPwnedAdapter adapter = crearAdapterApuntandoA(servidor);

        assertThat(adapter.estaComprometida(Password.de("contraseñaDistinta123"))).isFalse();
        assertThat(adapter.estaComprometida(Password.de(passwordFiltrada))).isTrue();
    }

    @Test
    void noDetectaComprometidaCuandoElSufijoNoAparece() throws Exception {
        servidor = iniciarServidorConRespuesta("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA:1");
        HaveIBeenPwnedAdapter adapter = crearAdapterApuntandoA(servidor);

        assertThat(adapter.estaComprometida(Password.de("contraseñaUnica123"))).isFalse();
    }

    @Test
    void failOpenCuandoElServicioNoResponde() {
        // Puerto 1 está reservado por el SO y rechaza la conexión inmediatamente.
        HaveIBeenPwnedAdapter adapter = new HaveIBeenPwnedAdapter("http://localhost:1/range/{prefijo}");

        assertThat(adapter.estaComprometida(Password.de("cualquierContraseña123"))).isFalse();
    }

    private static HaveIBeenPwnedAdapter crearAdapterApuntandoA(HttpServer servidor) {
        int puerto = servidor.getAddress().getPort();
        return new HaveIBeenPwnedAdapter("http://localhost:" + puerto + "/range/{prefijo}");
    }

    private static HttpServer iniciarServidorConRespuesta(String cuerpoRespuesta) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            byte[] cuerpo = cuerpoRespuesta.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, cuerpo.length);
            exchange.getResponseBody().write(cuerpo);
            exchange.close();
        });
        servidor.start();
        return servidor;
    }

    private static String sha1Hex(String valor) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }
}
