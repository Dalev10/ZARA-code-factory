package com.codefactory.supplychain.qa.funcionales.happypath;

import com.codefactory.supplychain.qa.soporte.AbstractCasoPruebaIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * HU-14 (Sprint 1). El defecto D-04 pedía baja lógica de CD, que no está en el plan: el CD
 * sin dependencias se elimina; con dependencias (prioridades, inventario) la eliminación se
 * bloquea y su información se conserva.
 */
@Tag("funcional")
@Tag("happy-path")
class EliminarCentroDistribucionIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — Eliminar Centro de Distribución existente")
    void cpHu14001_eliminarCentroDistribucionExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "La Estrella, Antioquia"}
                        """.formatted(unico("CD HU14-001")))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/centros-distribucion/" + cdId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(404);
    }
}
