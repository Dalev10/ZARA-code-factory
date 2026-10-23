package com.codefactory.supplychain.qa.funcionales.negativepath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * HU-35 (Sprint 2, D-52). Todo template exige categoriaId. SIN_CATEGORIA es un nombre reservado:
 * crearla o renombrar otra hacia ese nombre responde 409, sin distinguir mayúsculas.
 */
@Tag("funcional")
@Tag("negative-path")
class CategoriaObligatoriaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-001 — Rechazar un template sin categoría (400 con mensaje)")
    void cpHu35001_rechazarTemplateSinCategoria() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "temporada": "OI-2026", "precioBase": 49900}
                        """.formatted(unico("Template HU35-001")))
                .when().post("/templates")
                .then().statusCode(400)
                .body("mensaje", notNullValue());
    }

    @Test
    @DisplayName("CP-003 — Rechazar crear la categoría reservada SIN_CATEGORIA, sin distinguir mayúsculas (409)")
    void cpHu35003_rechazarCrearCategoriaReservada() {
        String tokenAdmin = iniciarSesionComoAdmin();

        for (String nombre : new String[] {"SIN_CATEGORIA", "sin_categoria", "Sin_Categoria"}) {
            given().header("Authorization", bearer(tokenAdmin))
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s"}
                            """.formatted(nombre))
                    .when().post("/categorias")
                    .then().statusCode(409)
                    .body("mensaje", notNullValue());
        }
    }

    @Test
    @DisplayName("CP-004 — Rechazar renombrar una categoría hacia SIN_CATEGORIA (409)")
    void cpHu35004_rechazarRenombrarHaciaReservada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String categoriaId = crearCategoria(tokenAdmin, "Cat HU35-004");

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "sin_categoria"}
                        """)
                .when().put("/categorias/" + categoriaId)
                .then().statusCode(409);
    }

    @Test
    @DisplayName("CP-005 — Rechazar un template con una categoría inexistente")
    void cpHu35005_rechazarTemplateConCategoriaInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "categoriaId": "%s"}
                        """.formatted(unico("Template HU35-005"), java.util.UUID.randomUUID()))
                .when().post("/templates")
                .then().statusCode(404);
    }
}
