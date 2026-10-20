package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-14 (Sprint 1). El defecto D-04 pedía baja lógica de CD, que no está en el plan: el CD
 * sin dependencias se elimina; con dependencias (prioridades, inventario) la eliminación se
 * bloquea y su información se conserva.
 */
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

    /**
     * Desde HU-51 un CD con prioridades asignadas no se elimina, lo que vuelve ejecutable este
     * caso vía API (en el Sprint 1 no había dependencias que construir).
     */
    @Test
    @DisplayName("CP-002 — Bloquear eliminación de CD con prioridades asignadas (409, HU-51)")
    void cpHu14002_bloquearEliminacionConDependenciasAsociadas() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String cdId = crearCdConPrioridad(tokenAdmin, "CD HU14-002");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/centros-distribucion/" + cdId)
                .then().statusCode(409)
                .body("mensaje", containsString("prioridades asignadas"));
    }

    @Test
    @DisplayName("CP-003 — Un CD con dependencias conserva su información tras el intento de eliminarlo")
    void cpHu14003_conservarInformacionHistorica() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String cdId = crearCdConPrioridad(tokenAdmin, "CD HU14-003");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().delete("/centros-distribucion/" + cdId)
                .then().statusCode(409);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/centros-distribucion/" + cdId)
                .then().statusCode(200)
                .body("id", equalTo(cdId));
    }

    private String crearCdConPrioridad(String tokenAdmin, String prefijo) {
        String cdId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Guarne, Antioquia"}
                        """.formatted(unico(prefijo)))
                .when().post("/centros-distribucion")
                .then().statusCode(201)
                .extract().path("id");
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Rionegro, Antioquia"}
                        """.formatted(unico("Tienda " + prefijo)))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");
        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"cdIds": ["%s"]}
                        """.formatted(cdId))
                .when().put("/tiendas/" + tiendaId + "/prioridad-cd")
                .then().statusCode(200);
        return cdId;
    }
}
