package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * HU-07 (Sprint 1) actualizada por D-55 (HU-34, Sprint 2): la Bodega_Tienda ya no se
 * registra por API, se crea junto con su tienda.
 */
class RegistrarBodegaTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-001 — La Bodega_Tienda se crea automáticamente al registrar la Tienda")
    void cpHu07001_registrarBodegaTiendaValida() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Rionegro, Antioquia"}
                        """.formatted(unico("Tienda HU07-001")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/tienda/" + tiendaId)
                .then().statusCode(200)
                .body("tiendaId", equalTo(tiendaId));
    }

    @Test
    @DisplayName("CP-002 — Consultar la Bodega_Tienda de una Tienda inexistente responde 404")
    void cpHu07002_rechazarSinTiendaValida() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .when().get("/bodegas-tienda/tienda/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-003 — Registrar una Bodega_Tienda por API ya no está permitido (405)")
    void cpHu07003_rechazarBodegaTiendaDuplicada() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(UUID.randomUUID()))
                .when().post("/bodegas-tienda")
                .then().statusCode(405);
    }
}
