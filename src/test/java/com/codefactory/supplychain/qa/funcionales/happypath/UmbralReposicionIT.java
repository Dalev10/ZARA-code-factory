package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * HU-47 (Sprint 2). Umbral por variante o por categoría, nunca ambas ni ninguna. Mínimo > 0 y
 * objetivo > mínimo. Uno por alcance (409 si se repite). Crear o cambiar un umbral no repone por
 * sí solo.
 */
@Tag("funcional")
@Tag("happy-path")
class UmbralReposicionIT extends AbstractCasoPruebaSprint2IT {

    private Response crear(String token, String cuerpo) {
        return given().header("Authorization", bearer(token))
                .contentType("application/json")
                .body(cuerpo)
                .when().post("/umbrales-reposicion");
    }

    @Test
    @DisplayName("CP-001 — Crear un umbral por variante y consultarlo")
    void cpHu47001_crearUmbralPorVariante() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU47-001");

        String id = crear(tokenAdmin, """
                {"varianteId": "%s", "cantidadMinima": 3, "cantidadObjetivo": 8}
                """.formatted(variante.id()))
                .then().statusCode(201)
                .body("varianteId", equalTo(variante.id()))
                .body("categoriaId", nullValue())
                .extract().path("id");

        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/umbrales-reposicion/" + id)
                .then().statusCode(200)
                .body("cantidadMinima", equalTo(3))
                .body("cantidadObjetivo", equalTo(8));
    }

    @Test
    @DisplayName("CP-002 — Crear un umbral por categoría")
    void cpHu47002_crearUmbralPorCategoria() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String categoriaId = crearCategoria(tokenAdmin, "Cat HU47-002");

        crear(tokenAdmin, """
                {"categoriaId": "%s", "cantidadMinima": 2, "cantidadObjetivo": 5}
                """.formatted(categoriaId))
                .then().statusCode(201)
                .body("categoriaId", equalTo(categoriaId))
                .body("varianteId", nullValue());
    }

    @Test
    @DisplayName("CP-006 — Modificar las cantidades de un umbral y eliminarlo")
    void cpHu47006_modificarYEliminar() {
        String tokenAdmin = iniciarSesionComoAdmin();
        Variante variante = crearVariante(tokenAdmin, "HU47-006");
        String id = crearUmbralVariante(tokenAdmin, variante.id(), 2, 5);

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"cantidadMinima": 4, "cantidadObjetivo": 10}
                        """)
                .when().put("/umbrales-reposicion/" + id)
                .then().statusCode(200)
                .body("cantidadMinima", equalTo(4))
                .body("cantidadObjetivo", equalTo(10));

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"cantidadMinima": 10, "cantidadObjetivo": 4}
                        """)
                .when().put("/umbrales-reposicion/" + id)
                .then().statusCode(400);

        given().header("Authorization", bearer(tokenAdmin))
                .when().delete("/umbrales-reposicion/" + id)
                .then().statusCode(204);
        given().header("Authorization", bearer(tokenAdmin))
                .when().get("/umbrales-reposicion/" + id)
                .then().statusCode(404);
    }
}
