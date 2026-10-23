package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaSprint2IT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-35 (Sprint 2, D-52). Todo template exige categoriaId. SIN_CATEGORIA es un nombre reservado:
 * crearla o renombrar otra hacia ese nombre responde 409, sin distinguir mayúsculas.
 */
@Tag("funcional")
@Tag("happy-path")
class CategoriaObligatoriaIT extends AbstractCasoPruebaSprint2IT {

    @Test
    @DisplayName("CP-002 — Crear un template con categoría la conserva")
    void cpHu35002_crearTemplateConCategoria() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String categoriaId = crearCategoria(tokenAdmin, "Cat HU35-002");

        given().header("Authorization", bearer(tokenAdmin))
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "categoriaId": "%s"}
                        """.formatted(unico("Template HU35-002"), categoriaId))
                .when().post("/templates")
                .then().statusCode(201)
                .body("categoriaId", equalTo(categoriaId));
    }
}
