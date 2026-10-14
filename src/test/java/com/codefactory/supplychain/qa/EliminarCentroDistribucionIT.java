package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

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

    @Test
    @Disabled("No ejecutable: no es posible construir en Sprint 1 un escenario real de "
            + "'operaciones o información asociada' a un Centro de Distribución para "
            + "ejercer esta precondición vía API. Revisión de código: "
            + "CentroDistribucionService.eliminarCentroDistribucion() no implementa ningún "
            + "guard de dependencias (ver defecto D-02 en el informe de ejecución) — pero "
            + "eso es un hallazgo de revisión de código, no algo reproducible en runtime "
            + "todavía.")
    @DisplayName("CP-002 — Bloquear eliminación de CD con operaciones/dependencias asociadas")
    void cpHu14002_bloquearEliminacionConDependenciasAsociadas() {
    }

    @Test
    @DisplayName("CP-003 — Conservar información histórica tras eliminar un CD [defecto D-04]")
    void cpHu14003_conservarInformacionHistorica() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Guarne, Antioquia"}
                        """.formatted(unico("CD HU14-003")))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/centros-distribucion/" + cdId)
                .then().statusCode(204);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .body("id", equalTo(cdId));
    }
}
