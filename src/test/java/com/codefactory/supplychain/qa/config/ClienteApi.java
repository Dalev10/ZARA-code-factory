package com.codefactory.supplychain.qa.config;

import com.codefactory.supplychain.qa.soporte.ContextoPrueba;
import io.cucumber.spring.ScenarioScope;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@Component
@ScenarioScope
public class ClienteApi {

    public static final String ADMIN_EMAIL = "admin@supplychain.local";
    public static final String ADMIN_PASSWORD = "CambiarInmediatamente2026!";
    public static final String PASSWORD_QA = "ClaveSegura-QA-2026!";

    private final ContextoPrueba contexto;

    public ClienteApi(ContextoPrueba contexto, @Value("${local.server.port}") int puerto) {
        this.contexto = contexto;
        RestAssured.port = puerto;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api/v1";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public Response get(String ruta) {
        return guardar(conToken().when().get(ruta));
    }

    public Response get(String ruta, Map<String, ?> query) {
        return guardar(conToken().queryParams(query).when().get(ruta));
    }

    public Response post(String ruta, Object cuerpo) {
        return guardar(conToken().body(cuerpo).when().post(ruta));
    }

    public Response put(String ruta, Object cuerpo) {
        return guardar(conToken().body(cuerpo).when().put(ruta));
    }

    public Response delete(String ruta) {
        return guardar(conToken().when().delete(ruta));
    }

    public Response postSinAuth(String ruta, Object cuerpo) {
        return guardar(given().contentType("application/json").body(cuerpo).when().post(ruta));
    }

    public Response getConToken(String token, String ruta) {
        return guardar(given().header("Authorization", "Bearer " + token).when().get(ruta));
    }

    public Response postConToken(String token, String ruta, Object cuerpo) {
        return guardar(given().header("Authorization", "Bearer " + token)
                .contentType("application/json").body(cuerpo).when().post(ruta));
    }

    public String iniciarSesion(String email, String password) {
        Response respuesta = postSinAuth("/auth/login", Map.of("email", email, "password", password));
        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException("No se pudo iniciar sesión como " + email
                    + ": " + respuesta.statusCode() + " " + respuesta.asString());
        }
        return respuesta.path("accessToken");
    }

    public String iniciarSesionComoAdmin() {
        return iniciarSesion(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private RequestSpecification conToken() {
        RequestSpecification spec = given().contentType("application/json");
        if (contexto.getToken() != null) {
            spec.header("Authorization", "Bearer " + contexto.getToken());
        }
        return spec;
    }

    private Response guardar(Response respuesta) {
        contexto.setRespuesta(respuesta);
        return respuesta;
    }
}
