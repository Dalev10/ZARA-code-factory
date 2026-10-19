package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-09 (Sprint 1) actualizada por D-55 (HU-34, Sprint 2): la Bodega_Tienda pertenece a
 * su tienda y no se reasigna por API.
 */
class ModificarBodegaTiendaIT extends AbstractCasoPruebaIT {

    private static String tiendaId;
    private static String bodegaId;

    private void asegurarFixture(String tokenAdmin) {
        if (bodegaId == null) {
            tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Girardota, Antioquia"}
                            """.formatted(unico("Tienda HU09")))
                    .when().post("/tiendas")
                    .then().statusCode(201)
                    .extract().path("id");
            bodegaId = bodegaDeTienda(tokenAdmin, tiendaId);
        }
    }

    @Test
    @DisplayName("CP-001 — Modificar una Bodega_Tienda por API ya no está permitido (405)")
    void cpHu09001_modificarBodegaReasignandoATiendaExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().put("/bodegas-tienda/" + bodegaId)
                .then().statusCode(405);
    }

    @Test
    @DisplayName("CP-002 — La Bodega_Tienda sigue asociada a su Tienda")
    void cpHu09002_rechazarModificacionATiendaInexistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
    }
}
