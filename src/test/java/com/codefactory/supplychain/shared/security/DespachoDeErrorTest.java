package com.codefactory.supplychain.shared.security;

import com.codefactory.supplychain.soporte.UsuariosDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con servidor real, Spring reenvía los errores a /error en otro despacho. MockMvc no lo
 * reproduce, por eso esta prueba levanta el servidor: un usuario autenticado debe recibir
 * el estado real (405, 404) y no un 401 del despacho de error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(UsuariosDePrueba.class)
class DespachoDeErrorTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int puerto;

    @Autowired
    private UsuariosDePrueba usuarios;

    @Test
    void unUsuarioAutenticadoRecibeElEstadoRealDelError() throws Exception {
        String bearer = usuarios.conRol("ADMIN").bearer();

        assertThat(enviar("DELETE", "/api/v1/bodegas-tienda/" + java.util.UUID.randomUUID(), bearer)).isEqualTo(405);
        assertThat(enviar("GET", "/api/v1/ruta-inexistente", bearer)).isEqualTo(404);
    }

    @Test
    void sinTokenSigueRespondiendo401() throws Exception {
        assertThat(enviar("GET", "/api/v1/ruta-inexistente", null)).isEqualTo(401);
    }

    private int enviar(String metodo, String ruta, String bearer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .method(metodo, HttpRequest.BodyPublishers.noBody());
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        try (HttpClient cliente = HttpClient.newHttpClient()) {
            return cliente.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }
}
