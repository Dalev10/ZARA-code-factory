package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-08 — Consultar Bodega_Tienda. Casos CP-HU08-001 y CP-HU08-002.
 */
class Hu08ConsultarBodegaTiendaIT extends AbstractCasoPruebaIT {

    private static String tiendaId;
    private static String bodegaId;
    private static final String NOMBRE_TIENDA = unico("Tienda HU08");

    private void asegurarFixture(String tokenAdmin) {
        if (tiendaId == null) {
            tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Copacabana, Antioquia"}
                            """.formatted(NOMBRE_TIENDA))
                    .when().post("/tiendas")
                    .then().statusCode(201)
                    .extract().path("id");

            bodegaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"tiendaId": "%s"}
                            """.formatted(tiendaId))
                    .when().post("/bodegas-tienda")
                    .then().statusCode(201)
                    .extract().path("id");
        }
    }

    @Test
    @DisplayName("CP-HU08-001 — Consultar Bodega_Tienda existente (muestra la Tienda a la que pertenece)")
    void cpHu08001_consultarBodegaTiendaExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId))
                .body("tiendaNombre", equalTo(NOMBRE_TIENDA));
    }

    @Test
    @DisplayName("CP-HU08-002 — Identificar el inventario asociado a la Bodega_Tienda")
    void cpHu08002_identificarInventarioAsociado() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        // Igual que en CP-HU04-003: el campo "inventario" existe y es consultable,
        // pero está siempre vacío porque el módulo de inventario es Sprint 2.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("inventario", empty());
    }
}
