package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-04 — Consultar Tienda/Almacén. Casos CP-HU04-001 a CP-HU04-003.
 *
 * <p>Fixture propia: una Tienda con su Bodega_Tienda asociada, creada una
 * sola vez de forma perezosa y reutilizada por los 3 casos.
 */
class Hu04ConsultarTiendaIT extends AbstractCasoPruebaIT {

    private static String tiendaId;
    private static String bodegaId;
    private static final String NOMBRE_TIENDA = unico("Tienda HU04");

    private void asegurarFixture(String tokenAdmin) {
        if (tiendaId == null) {
            tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                    .contentType("application/json")
                    .body("""
                            {"nombre": "%s", "ubicacion": "Envigado, Antioquia"}
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
    @DisplayName("CP-HU04-001 — Consultar Tienda existente (información básica y ubicación)")
    void cpHu04001_consultarTiendaExistente() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/tiendas/" + tiendaId)
                .then().statusCode(200)
                .body("nombre", equalTo(NOMBRE_TIENDA))
                .body("ubicacion", equalTo("Envigado, Antioquia"));
    }

    @Test
    @DisplayName("CP-HU04-002 — Consultar Tienda e identificar la Bodega_Tienda asociada")
    void cpHu04002_identificarBodegaTiendaAsociada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/tienda/" + tiendaId)
                .then().statusCode(200)
                .body("id", equalTo(bodegaId))
                .body("tiendaNombre", equalTo(NOMBRE_TIENDA));
    }

    @Test
    @DisplayName("CP-HU04-003 — Consultar la información de inventario correspondiente a la Tienda")
    void cpHu04003_consultarInformacionDeInventario() {
        String tokenAdmin = iniciarSesionComoAdmin();
        asegurarFixture(tokenAdmin);

        // El inventario de la Tienda solo es consultable de forma indirecta, vía su
        // Bodega_Tienda asociada. Aparece siempre vacío porque el módulo de
        // inventario/movimientos es alcance de Sprint 2 (Plan de Calidad, Incremento 2):
        // esto es una limitación de alcance documentada, no un defecto.
        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/" + bodegaId)
                .then().statusCode(200)
                .body("inventario", empty());
    }
}
