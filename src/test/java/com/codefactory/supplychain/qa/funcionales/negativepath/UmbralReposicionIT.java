package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-47 (Sprint 2). Umbral por variante o por categoría, nunca ambas ni ninguna. Mínimo > 0 y
 * objetivo > mínimo. Uno por alcance (409 si se repite). Crear o cambiar un umbral no repone por
 * sí solo.
 */
@Tag("funcional")
@Tag("negative-path")
class UmbralReposicionIT extends AbstractCasoPruebaSprint2IT {

    private Response crear(String token, String cuerpo) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(cuerpo)
                .when().post("/umbrales-reposicion");
    }

    @Test
    @DisplayName("CP-003 — Rechazar un umbral con variante y categoría a la vez, o sin ninguna (400)")
    void cpHu47003_rechazarAlcanceInvalido() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU47-003");

        crear(tokenAdmin, """
                {"varianteId": "%s", "categoriaId": "%s", "cantidadMinima": 2, "cantidadObjetivo": 5}
                """.formatted(variante.id(), variante.categoriaId()))
                .then().statusCode(400).body("mensaje", notNullValue());
        crear(tokenAdmin, """
                {"cantidadMinima": 2, "cantidadObjetivo": 5}
                """)
                .then().statusCode(400).body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-004 — Rechazar mínimo ≤ 0 u objetivo ≤ mínimo (400)")
    void cpHu47004_rechazarCantidadesInvalidas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU47-004");

        crear(tokenAdmin, """
                {"varianteId": "%s", "cantidadMinima": 0, "cantidadObjetivo": 5}
                """.formatted(variante.id())).then().statusCode(400);
        crear(tokenAdmin, """
                {"varianteId": "%s", "cantidadMinima": 5, "cantidadObjetivo": 5}
                """.formatted(variante.id())).then().statusCode(400);
        crear(tokenAdmin, """
                {"varianteId": "%s", "cantidadMinima": 6, "cantidadObjetivo": 5}
                """.formatted(variante.id())).then().statusCode(400);
    }

    @Test
    @DisplayName("CP-005 — Rechazar un segundo umbral para el mismo alcance (409)")
    void cpHu47005_rechazarUmbralDuplicado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU47-005");
        crearUmbralVariante(tokenAdmin, variante.id(), 2, 5);

        crear(tokenAdmin, """
                {"varianteId": "%s", "cantidadMinima": 3, "cantidadObjetivo": 9}
                """.formatted(variante.id()))
                .then().statusCode(409).body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-008 — Un umbral sobre una variante inexistente responde 404")
    void cpHu47008_varianteInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();

        crear(tokenAdmin, """
                {"varianteId": "%s", "cantidadMinima": 2, "cantidadObjetivo": 5}
                """.formatted(UUID.randomUUID()))
                .then().statusCode(404);
    }
}
