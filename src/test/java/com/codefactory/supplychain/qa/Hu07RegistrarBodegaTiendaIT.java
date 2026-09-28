package com.codefactory.supplychain.qa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * HU-07 — Registrar Bodega_Tienda. Casos CP-HU07-001 a CP-HU07-003.
 */
class Hu07RegistrarBodegaTiendaIT extends AbstractCasoPruebaIT {

    @Test
    @DisplayName("CP-HU07-001 — Registrar Bodega_Tienda con la información requerida")
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
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201)
                .body("tiendaId", org.hamcrest.Matchers.equalTo(tiendaId));
    }

    @Test
    @DisplayName("CP-HU07-002 — Rechazar registro de Bodega_Tienda sin una Tienda asociada válida")
    void cpHu07002_rechazarSinTiendaValida() {
        String tokenAdmin = iniciarSesionComoAdmin();

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(UUID.randomUUID()))
                .when().post("/bodegas-tienda")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("CP-HU07-003 — Rechazar registro de Bodega_Tienda duplicada")
    void cpHu07003_rechazarBodegaTiendaDuplicada() {
        String tokenAdmin = iniciarSesionComoAdmin();
        String tiendaId = given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"nombre": "%s", "ubicacion": "Marinilla, Antioquia"}
                        """.formatted(unico("Tienda HU07-003")))
                .when().post("/tiendas")
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(201);

        given().header("Authorization", "Bearer " + tokenAdmin)
                .contentType("application/json")
                .body("""
                        {"tiendaId": "%s"}
                        """.formatted(tiendaId))
                .when().post("/bodegas-tienda")
                .then().statusCode(409);
    }
}
