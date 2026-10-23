package com.codefactory.supplychain.qa.nofuncionales.seguridad;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-47 (Sprint 2). Umbral por variante o por categoría, nunca ambas ni ninguna. Mínimo > 0 y
 * objetivo > mínimo. Uno por alcance (409 si se repite). Crear o cambiar un umbral no repone por
 * sí solo.
 */
@Tag("no-funcional")
@Tag("seguridad")
class UmbralReposicionIT extends AbstractCasoPruebaSprint2IT {

    private Response crear(String token, String cuerpo) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(cuerpo)
                .when().post("/umbrales-reposicion");
    }

    @Test
    @DisplayName("CP-009 — Administrar umbrales exige reposicion:administrar (403 para un supervisor)")
    void cpHu47009_exigeScope() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU47-009");
        Usuario supervisor = usuarioConRol(tokenAdmin, "hu47-009", "SUPERVISOR_TIENDA");

        crear(supervisor.token(), """
                {"varianteId": "%s", "cantidadMinima": 2, "cantidadObjetivo": 5}
                """.formatted(variante.id()))
                .then().statusCode(403);
    }
}
