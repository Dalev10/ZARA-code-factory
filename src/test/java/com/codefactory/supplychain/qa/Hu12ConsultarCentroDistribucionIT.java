package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-12 — Consultar Centro de Distribución. Casos CP-HU12-001 y CP-HU12-002.
 */
class Hu12ConsultarCentroDistribucionIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU12-001 — Consultar Centro de Distribución existente")
    void cpHu12001_consultarCentroDistribucionExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String nombre = unico("CD HU12-001");
        String cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Sabaneta, Antioquia"}
                        """.formatted(nombre))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .body("nombre", equalTo(nombre))
                .body("nodo.tipo", equalTo("CD"));
    }

    @Test
    @Disabled("No ejecutable: la respuesta de consulta de un Centro de Distribución "
            + "(CentroDistribucionConNodoResponse) no expone ningún campo de inventario, "
            + "y no existe un endpoint de inventario de CD en Sprint 1.")
    @DisplayName("CP-HU12-002 — Consultar la información de inventario asociada al CD")
    void cpHu12002_consultarInventarioAsociado() {
        // Intencionalmente vacío: ver motivo en @Disabled.
    }
}
